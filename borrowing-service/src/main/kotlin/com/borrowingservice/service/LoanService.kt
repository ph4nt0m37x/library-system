package com.borrowingservice.service

import com.borrowingservice.model.command.CreateLoanCommand
import com.borrowingservice.model.command.DeclareBookLostCommand
import com.borrowingservice.model.command.ExtendLoanCommand
import com.borrowingservice.model.command.RecordPermanentBookDamageCommand
import com.borrowingservice.model.command.ReturnLoanCommand
import java.util.concurrent.CompletableFuture

interface LoanService {
    fun createLoan(command: CreateLoanCommand): CompletableFuture<String>
    fun extendLoan(command: ExtendLoanCommand): CompletableFuture<String>
    fun returnLoan(command: ReturnLoanCommand): CompletableFuture<String>
    fun declareBookLost(command: DeclareBookLostCommand): CompletableFuture<String>
    fun recordPermanentBookDamage(command: RecordPermanentBookDamageCommand): CompletableFuture<String>
}
