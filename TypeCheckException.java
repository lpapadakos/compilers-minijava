public class TypeCheckException extends Exception {
	/* location: Contains name of offending class and method (if any),
	 * for pretty printing */
	public TypeCheckException(String className, String methodName, String msg) {
		super((className != null ? "In " + className + (methodName != null ? '.' + methodName + "()" : "") + ": " : "") + msg);
	}

	public TypeCheckException(String[] names, String msg) {
		this(names[0], names[1], msg);
	}

	public TypeCheckException(String containerName, String msg) {
		this(containerName, null, msg);
	}

	public TypeCheckException(String msg) {
		this(null, null, msg);
	}
}
