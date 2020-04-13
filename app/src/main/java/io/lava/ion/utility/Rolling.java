package io.lava.ion.utility;

import java.util.ArrayList;

public class Rolling {
	/* rolling average */
	// contrary to popular belief, this class is unrelated to MDMA
	
    ArrayList<Double> runningTotal = new ArrayList<Double>();

    public Rolling(int size, double initialValue) {
        for (int i = 0; i < size; i++) runningTotal.add(initialValue);
    }

    public void add(double x) {
    	runningTotal.remove(0);
    	runningTotal.add(x);
    }

    public double getAverage() {
    	double total = 0;
    	for (int i = 0; i<runningTotal.size(); i++)
    		total += runningTotal.get(i);
    	
        return total / runningTotal.size();
    }   
}