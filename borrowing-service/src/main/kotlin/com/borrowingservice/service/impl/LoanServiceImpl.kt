package com.borrowingservice.service.impl

import com.borrowingservice.model.command.CreateLoanCommand
import com.borrowingservice.model.command.DeclareBookLostCommand
import com.borrowingservice.model.command.ExtendLoanCommand
import com.borrowingservice.model.command.RecordPermanentBookDamageCommand
import com.borrowingservice.model.command.ReturnLoanCommand
import com.borrowingservice.service.LoanService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class LoanServiceImpl(
    private val commandGateway: CommandGateway
) : LoanService {
    override fun createLoan(command: CreateLoanCommand): CompletableFuture<String> = commandGateway.send(command)

    override fun extendLoan(command: ExtendLoanCommand): CompletableFuture<String> = commandGateway.send(command)

    override fun returnLoan(command: ReturnLoanCommand): CompletableFuture<String> = commandGateway.send(command)

    override fun declareBookLost(command: DeclareBookLostCommand): CompletableFuture<String> =
        commandGateway.send(command)

    override fun recordPermanentBookDamage(
        command: RecordPermanentBookDamageCommand
    ): CompletableFuture<String> = commandGateway.send(command)
}
