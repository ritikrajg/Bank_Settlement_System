package com.iispl.entity;

import java.time.LocalDate;

import com.iispl.enums.ChannelType;
import com.iispl.enums.InstructionStatus;

/**
 * An instruction dispatched to a payment rail (RTGS / NEFT / SWIFT / UPI).
 * Created by the NettingEngine after positions are finalised.
 */
public class SettlementInstruction extends BaseEntity {

	private String instructionId;
	private Long transactionId; // FK → IncomingTransaction
	private String instructionType; // e.g. CREDIT_TRANSFER / DIRECT_DEBIT
	private ChannelType channel;
	private int priority; // 1 = highest
	private LocalDate valueDate;
	private Long senderBankId;
	private Long receiverBankId;
	private InstructionStatus instructionStatus;

	public SettlementInstruction() {
	}

	public SettlementInstruction(String instructionId, Long transactionId, String instructionType, ChannelType channel,
			int priority, LocalDate valueDate, Long senderBankId, Long receiverBankId) {
		this.instructionId = instructionId;
		this.transactionId = transactionId;
		this.instructionType = instructionType;
		this.channel = channel;
		this.priority = priority;
		this.valueDate = valueDate;
		this.senderBankId = senderBankId;
		this.receiverBankId = receiverBankId;
		this.instructionStatus = InstructionStatus.PENDING;
	}

	// Getters / Setters
	public String getInstructionId() {
		return instructionId;
	}

	public void setInstructionId(String instructionId) {
		this.instructionId = instructionId;
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public String getInstructionType() {
		return instructionType;
	}

	public void setInstructionType(String instructionType) {
		this.instructionType = instructionType;
	}

	public ChannelType getChannel() {
		return channel;
	}

	public void setChannel(ChannelType channel) {
		this.channel = channel;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	public Long getSenderBankId() {
		return senderBankId;
	}

	public void setSenderBankId(Long senderBankId) {
		this.senderBankId = senderBankId;
	}

	public Long getReceiverBankId() {
		return receiverBankId;
	}

	public void setReceiverBankId(Long receiverBankId) {
		this.receiverBankId = receiverBankId;
	}

	public InstructionStatus getInstructionStatus() {
		return instructionStatus;
	}

	public void setInstructionStatus(InstructionStatus s) {
		this.instructionStatus = s;
	}

	@Override
	public String toString() {
		return "SettlementInstruction{id=" + instructionId + ", channel=" + channel + ", priority=" + priority
				+ ", valueDate=" + valueDate + ", status=" + instructionStatus + "}";
	}
}
