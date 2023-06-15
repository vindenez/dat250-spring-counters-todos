package no.hvl.dat250.rest.counters;

public class Counters {

	private final int red;
	private final int green;

	public Counters () {
		this.red = 0;
		this.green = 0;
	}

	public Counters (int red, int green) {
		this.red = red;
		this.green = green;
	}

	public int getRed() {
		return red;
	}

	public int getGreen() {
		return green;
	}
}
