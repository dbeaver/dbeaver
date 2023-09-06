package org.jkiss.dbeaver.debug.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A mockup ProcessHandle which works in conjunction with {@link MockProcess}.
 */
public class MockProcessHandle implements ProcessHandle {

	private final MockProcess process;
	private final Collection<ProcessHandle> children;
	private final CompletableFuture<ProcessHandle> onExit = new CompletableFuture<>();

	/**
	 * Create new mockup process handle for a process without children.
	 *
	 * @param process the process of this handle
	 */
	public MockProcessHandle(MockProcess process) {
		this(process, Collections.emptyList());
	}

	/**
	 * Create new mockup process handle for a process with the given children.
	 *
	 * @param process the process of this handle
	 * @param children the child-processes of the given process
	 */
	public MockProcessHandle(MockProcess process, Collection<Process> children) {
		this.process = process;
		this.children = children.stream().map(Process::toHandle).collect(Collectors.toUnmodifiableList());
	}

	@Override
	public Stream<ProcessHandle> children() {
		return this.children.stream();
	}

	@Override
	public Stream<ProcessHandle> descendants() {
		return Stream.concat(children(), children().flatMap(ProcessHandle::descendants));
	}

	@Override
	public CompletableFuture<ProcessHandle> onExit() {
		return onExit; // onExit must be completed by process
	}

	@Override
	public boolean supportsNormalTermination() {
		return true;
	}

	@Override
	public boolean destroy() {
		process.destroy();
		return true;
	}

	@Override
	public boolean destroyForcibly() {
		return destroy();
	}

	@Override
	public boolean isAlive() {
		return process.isAlive();
	}

	@Override
	public int compareTo(ProcessHandle other) {
		return Long.compare(pid(), ((MockProcessHandle) other).pid());
	}

	/**
	 * Notify this handle that this mock-process has terminated. If this is not
	 * called handles of descendant processes do not know about the termination
	 * (and for descendants only the handle is queried, not the process itself).
	 */
	void setTerminated() {
		onExit.complete(this);
	}

	// not yet implemented methods

	@Override
	public long pid() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Optional<ProcessHandle> parent() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Info info() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
