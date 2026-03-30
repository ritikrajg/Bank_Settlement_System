package com.iispl.entity;

public interface Processable {

	/**
	 * Executes the core processing logic for this object. Implementations should be
	 * idempotent where possible.
	 */
	void process();

	/**
	 * Returns true if this object is in a state that allows processing.
	 */
	boolean canProcess();
}
