package dev.aleiis.hintforge.eval;

public class MRRAtKCalculator {
	
	private double sum = 0;
	private int n = 0;
	private int k;
	
	public MRRAtKCalculator(int k) {
		this.k = k;
	}
	
	public void score(int position) {
		if (position < 1) {
			throw new IllegalArgumentException("The position must be 1 or greater");
		}
		
		if (position <= k) {
			this.sum += 1.0 / position;
		}
		
		this.n += 1;
	}
	
	public void score() {
		this.n += 1;
	}
	
	public double collect() {
		return sum / n;
	}
}
