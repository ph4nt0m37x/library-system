[CmdletBinding()]
param(
    [string]$GatewayBaseUrl = "http://localhost:8000",
    [string]$KeycloakBaseUrl = "http://localhost:8180",
    [string]$ClientId = "mcp-smoke-client",
    [string]$ClientSecret = $env:MCP_SMOKE_CLIENT_SECRET,
    [string]$Origin
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ClientSecret)) {
    $ClientSecret = "mcp-smoke-secret"
}

$tokenEndpoint = "$($KeycloakBaseUrl.TrimEnd('/'))/realms/library-system/protocol/openid-connect/token"
$mcpEndpoint = "$($GatewayBaseUrl.TrimEnd('/'))/mcp"

Write-Host "Obtaining an admin service-account token from Keycloak..."
$tokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri $tokenEndpoint `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        client_id     = $ClientId
        client_secret = $ClientSecret
        grant_type    = "client_credentials"
    }

if ([string]::IsNullOrWhiteSpace($tokenResponse.access_token)) {
    throw "Keycloak did not return an access token."
}

$script:mcpSessionId = $null
$script:requestId = 0

function ConvertFrom-McpResponse {
    param([Parameter(Mandatory)] [string]$Content)

    $trimmed = $Content.Trim()
    if ($trimmed.StartsWith("{")) {
        return $trimmed | ConvertFrom-Json
    }

    $dataLine = $trimmed -split "`r?`n" |
        Where-Object { $_ -match '^data:\s*(.+)$' } |
        Select-Object -First 1

    if ($null -eq $dataLine) {
        throw "The MCP server returned neither JSON nor a JSON-bearing SSE event: $trimmed"
    }

    $json = ([regex]::Match($dataLine, '^data:\s*(.+)$')).Groups[1].Value
    return $json | ConvertFrom-Json
}

function Invoke-McpRequest {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [hashtable]$Params = @{},
        [switch]$Notification
    )

    $headers = @{
        Authorization = "Bearer $($tokenResponse.access_token)"
        Accept        = "application/json, text/event-stream"
    }
    if ($script:mcpSessionId) {
        $headers["Mcp-Session-Id"] = $script:mcpSessionId
    }
    if (-not [string]::IsNullOrWhiteSpace($Origin)) {
        $headers["Origin"] = $Origin
    }

    $payload = [ordered]@{
        jsonrpc = "2.0"
        method  = $Method
        params  = $Params
    }
    if (-not $Notification) {
        $script:requestId++
        $payload["id"] = $script:requestId
    }

    $response = Invoke-WebRequest `
        -Method Post `
        -Uri $mcpEndpoint `
        -Headers $headers `
        -ContentType "application/json" `
        -Body ($payload | ConvertTo-Json -Depth 20)

    if (-not $script:mcpSessionId -and $response.Headers["Mcp-Session-Id"]) {
        $script:mcpSessionId = [string]$response.Headers["Mcp-Session-Id"]
    }

    if ($Notification) {
        return $null
    }

    $message = ConvertFrom-McpResponse -Content $response.Content
    if ($message.error) {
        throw "MCP $Method failed ($($message.error.code)): $($message.error.message)"
    }
    return $message.result
}

Write-Host "Initializing an MCP Streamable HTTP session through the API Gateway..."
$initialize = Invoke-McpRequest -Method "initialize" -Params @{
    protocolVersion = "2025-11-25"
    capabilities    = @{}
    clientInfo      = @{
        name    = "library-mcp-smoke-test"
        version = "1.0.0"
    }
}

if ($initialize.serverInfo.name -ne "library-system") {
    throw "Unexpected MCP server name '$($initialize.serverInfo.name)'."
}

Invoke-McpRequest -Method "notifications/initialized" -Notification

$tools = Invoke-McpRequest -Method "tools/list"
$resources = Invoke-McpRequest -Method "resources/list"
$templates = Invoke-McpRequest -Method "resources/templates/list"
$prompts = Invoke-McpRequest -Method "prompts/list"

$expectedToolNames = @(
    "catalog_search",
    "catalog_get_book",
    "inventory_find_copies",
    "member_get_account_summary",
    "loan_get",
    "fee_list_unpaid",
    "loan_create",
    "loan_extend",
    "loan_return",
    "loan_report_lost",
    "loan_report_damage",
    "payment_quote",
    "payment_record"
)
$actualToolNames = @($tools.tools | ForEach-Object { $_.name } | Sort-Object)
$missingTools = @($expectedToolNames | Where-Object { $_ -notin $actualToolNames })

if ($actualToolNames.Count -ne 13 -or $missingTools.Count -gt 0) {
    throw "Expected 13 MCP tools. Actual=$($actualToolNames.Count); missing=$($missingTools -join ', ')."
}
if (@($resources.resources).Count -ne 1) {
    throw "Expected 1 fixed resource; found $(@($resources.resources).Count)."
}
if (@($templates.resourceTemplates).Count -ne 3) {
    throw "Expected 3 resource templates; found $(@($templates.resourceTemplates).Count)."
}
if (@($prompts.prompts).Count -ne 3) {
    throw "Expected 3 prompts; found $(@($prompts.prompts).Count)."
}

Write-Host "MCP smoke test passed."
Write-Host "  Server: $($initialize.serverInfo.name) $($initialize.serverInfo.version)"
Write-Host "  Protocol: $($initialize.protocolVersion)"
Write-Host "  Tools (13): $($actualToolNames -join ', ')"
Write-Host "  Fixed resources: 1; resource templates: 3; prompts: 3"

