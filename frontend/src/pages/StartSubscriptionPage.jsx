import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import keycloak from "../keycloak";

function StartSubscriptionPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        tier: "THREE_MONTHS",
        startsAt: "",
        amountPaid: "",
        currency: "EUR",
        paidAt: "",
        paymentReference: "",
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleChange = (e) => {
        const { name, value } = e.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            setLoading(true);
            setError("");

            const body = {
                tier: form.tier,
                startsAt: new Date(form.startsAt).toISOString(),
                amountPaid: Number(form.amountPaid),
                currency: form.currency,
                paidAt: new Date(form.paidAt).toISOString(),
                paymentReference: form.paymentReference,
            };

            console.log(
                "START SUBSCRIPTION BODY:",
                JSON.stringify(body, null, 2)
            );

            const response = await fetch(
                `http://localhost:8000/api/members/${id}/subscriptions/start`,
{
    method: "POST",
        headers: {
    "Content-Type": "application/json",
        Authorization: `Bearer ${keycloak.token}`,
},
    body: JSON.stringify(body),
}
);

if (!response.ok) {
    const message = await response.text();

    throw new Error(
        message || "Failed to start subscription."
    );
}

navigate(`/members/${id}`);
} catch (err) {
    console.error(err);
    setError(
        err.message || "Failed to start subscription."
    );
} finally {
    setLoading(false);
}
};

return (
    <div>
        <button
            type="button"
            onClick={() => navigate(`/members/${id}`)}
            disabled={loading}
        >
            ← Back to Member
        </button>

        <h1>Start Subscription</h1>

        {error && <p>{error}</p>}

        <form onSubmit={handleSubmit}>
            <div>
                <label htmlFor="tier">Subscription tier</label>

                <select
                    id="tier"
                    name="tier"
                    value={form.tier}
                    onChange={handleChange}
                    required
                >
                    <option value="THREE_MONTHS">
                        3 Months
                    </option>
                    <option value="SIX_MONTHS">
                        6 Months
                    </option>
                    <option value="TWELVE_MONTHS">
                        12 Months
                    </option>
                </select>
            </div>

            <div>
                <label htmlFor="startsAt">Starts at</label>

                <input
                    id="startsAt"
                    name="startsAt"
                    type="datetime-local"
                    value={form.startsAt}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label htmlFor="amountPaid">Amount paid</label>

                <input
                    id="amountPaid"
                    name="amountPaid"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.amountPaid}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label htmlFor="currency">Currency</label>

                <input
                    id="currency"
                    name="currency"
                    type="text"
                    value={form.currency}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label htmlFor="paidAt">Paid at</label>

                <input
                    id="paidAt"
                    name="paidAt"
                    type="datetime-local"
                    value={form.paidAt}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label htmlFor="paymentReference">
                    Payment reference
                </label>

                <input
                    id="paymentReference"
                    name="paymentReference"
                    type="text"
                    value={form.paymentReference}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <button
                    type="button"
                    onClick={() => navigate(`/members/${id}`)}
                    disabled={loading}
                >
                    Cancel
                </button>

                <button type="submit" disabled={loading}>
                    {loading
                        ? "Starting..."
                        : "Start Subscription"}
                </button>
            </div>
        </form>
    </div>
);
}

export default StartSubscriptionPage;
