package com.vrtx.ledger.controller;

import com.vrtx.ledger.dto.PaymentRequest;
import com.vrtx.ledger.dto.RefundRequest;
import com.vrtx.ledger.dto.SettlementRequest;
import com.vrtx.ledger.dto.TransactionResponse;
import com.vrtx.ledger.dto.TransferRequest;
import com.vrtx.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final LedgerService ledgerService;

    public TransactionController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> payment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.pay(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> refund(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.refund(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.transfer(request));
    }

    @PostMapping("/settlement")
    public ResponseEntity<TransactionResponse> settlement(@Valid @RequestBody SettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.settle(request));
    }
}
