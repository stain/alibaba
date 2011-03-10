
function fibonacci(fib) {
	if (fib <= 0)
		return 0;
	if (fib <= 1)
		return 1;
	return fibonacci(fib - 1) + fibonacci(fib - 2);
}

function test() {
	this.assertEquals(55, fibonacci(10));
}
