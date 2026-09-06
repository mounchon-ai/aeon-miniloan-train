package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * บันทึกคำสั่งที่ประมวลผลแล้ว (ENT-012 · BR-miniloan-043@v1). The unique constraint on
 * {@code (commandType, requestId)} IS the dedup mechanism — AC-miniloan-134 rules out a disabled
 * button or an in-memory check, and a second attempt has to fail on the database, including when
 * the two attempts arrive at the same instant.
 *
 * <p>ENT-012 answers DQ-miniloan-009 with the shape of the key but not with what {@code requestId}
 * holds per command. API-003 (POST /applications/{id}/submit) declares no request body and no
 * header, so this unit takes the application's own id as the requestId of a SubmitApplication:
 * submitting one application is by definition one command, and it needs no client contract nobody
 * has written down. A command that can legitimately repeat on the same aggregate (RecordPayment)
 * will need a client-supplied key, and that is the unit that builds it.
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_idempotency_command_request",
                        columnNames = {"commandType", "requestId"}))
public class IdempotencyKey {

    public enum CommandType {
        SubmitApplication,
        ApproveApplication,
        RejectApplication,
        CancelApplication,
        DisburseLoan,
        RecordPayment,
        /** FE-miniloan-014 · UC-miniloan-016 — a payoff is its own command, not a RecordPayment. */
        SettleEarly
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandType commandType;

    @Column(nullable = false)
    private String requestId;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private Instant processedAt;

    protected IdempotencyKey() {
        // JPA
    }

    public IdempotencyKey(CommandType commandType, String requestId, String aggregateId) {
        this.commandType = commandType;
        this.requestId = requestId;
        this.aggregateId = aggregateId;
        this.processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
