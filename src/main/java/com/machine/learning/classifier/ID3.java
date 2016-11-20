package com.machine.learning.classifier;

import com.machine.learning.model.DataPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class ID3 implements Classifier {

    List<DataPoint> trainingData = new ArrayList<>();
    List<DataPoint> validationData = new ArrayList<>();
    DecisionTree dt;
    Set<DecisionTree> subtrees = new HashSet<>();
    
    public class DecisionTree {
	int attributeIndex;
	String attributeValue;
	String clazz;
	String maxClass;
	
	DecisionTree pos;
	DecisionTree neg;

	public DecisionTree(int attrIndex, String attrValue) {
	    attributeIndex = attrIndex;
	    attributeValue = attrValue;
	}

	public DecisionTree(String clazz) {
	    this.clazz = clazz;
	}
	
    }

    @Override
    public void train(List<DataPoint> dataPoints) {
	// Seperate data into training and validation
	Collections.shuffle(dataPoints);
	trainingData.clear();
	validationData.clear();
	subtrees = new HashSet<>();
	trainingData.addAll(dataPoints.subList(0, (int)(0.6*dataPoints.size())));
	validationData.addAll(dataPoints.subList((int)(0.6*dataPoints.size()), dataPoints.size()));

	// Construct decision tree
	dt = constructDT(trainingData);

	findSubtrees(dt);
	// Prune decision tree
	pruneTree();

    }

    private void findSubtrees(DecisionTree subtree) {
	if(subtree.clazz == null) {
	    subtrees.add(subtree);
	
	    findSubtrees(subtree.pos);
	    findSubtrees(subtree.neg);
	}
    }

    private void pruneTree() {
	DecisionTree bestSubtree = null;
	
	do {
	    
	    int bestError = validationError(dt);
	    bestSubtree = null;
	    
	    //test on each of the subtree nodes
	    for (DecisionTree subtree : subtrees) {
		subtree.clazz = subtree.maxClass; //prune the node to be its majority class
		int newError = validationError(subtree);
		subtree.clazz = null; //unprune the node for now
		
		if (newError < bestError) {
		    bestError = newError;
		    bestSubtree = subtree;
		}
	    }
	    if (bestSubtree != null) {
		bestSubtree.clazz = bestSubtree.maxClass;
		bestSubtree.pos = bestSubtree.neg = null;
		removeSubtrees(bestSubtree);
	    }
	} while(bestSubtree != null);
    }

    private void removeSubtrees(DecisionTree subtree) {
	subtrees.remove(subtree);

	if(subtree.pos != null) {
	    removeSubtrees(subtree.pos);
	    removeSubtrees(subtree.neg);	    
	}
    }

    private Map<String, AtomicInteger> countClasses(DecisionTree dt, DecisionTree count) {
	Map<String, AtomicInteger> classCounts = new HashMap<>();

	for (DataPoint point : trainingData) {
	    String classLabel = point.getClassLabel().get();
	    if (!classCounts.containsKey(point.getClassLabel().get())) {
		classCounts.put(point.getClassLabel().get(), new AtomicInteger());
	    }
	    if (reaches(dt, count, point.getData().get())) {
		classCounts.get(classLabel).incrementAndGet();
	    }
	}

	return classCounts;
    }

    private boolean reaches(DecisionTree curDT, DecisionTree count, List dataPoint) {
	while (curDT.clazz == null) {
	    if (curDT == count) {
		return true;
	    }
	    if (dataPoint.get(curDT.attributeIndex).equals(curDT.attributeValue)) {
		curDT = curDT.pos;
	    } else {
		curDT = curDT.neg;
	    }
	}
	
	return false;
    }

    private int validationError(DecisionTree dt) {
	int errors = 0;
	for (DataPoint point : validationData) {
	    if (!classify(point.getData().get(), dt).equals(point.getClassLabel().get())) {
		errors++;
	    }
	}
	return errors;
    }

    @Override
    public String classify(List dataPoint) {
	return classify(dataPoint, dt);
    }

    private String classify(List dataPoint, DecisionTree curDT) {
	while (curDT.clazz == null) {
	    if (dataPoint.get(curDT.attributeIndex).equals(curDT.attributeValue)) {
		curDT = curDT.pos;
	    } else {
		curDT = curDT.neg;
	    }
	}
	
	return curDT.clazz;
    }

    public DecisionTree constructDT(List<DataPoint> remainingData) {
	if (remainingData == null || remainingData.size() == 0) {
	    return null;
	}
	Set<String> classes = new HashSet<>();
	List<Set<String>> usedAttrValues = new ArrayList<>();

	for (DataPoint dataPoint : remainingData) {
	    classes.add(dataPoint.getClassLabel().get());
	}

	if (classes.size() == 1) {
	    String singleClass = classes.iterator().next();
	    return new DecisionTree(singleClass);
	}
	

	for(int i = 0; i < remainingData.get(0).getData().get().size(); i++) {
	    usedAttrValues.add(new HashSet<String>());
	}

	for (DataPoint dataPoint : remainingData) {
	    List<String> datum = dataPoint.getData().get();
	    for (int i = 0; i < datum.size(); i++) { 
		usedAttrValues.get(i).add(datum.get(i));
	    }
	}

	int attrIndex = 0;
	String attributeValue = "";
	double minEntropy = Double.MAX_VALUE;
	ArrayList<DataPoint> posData = new ArrayList<>();
	ArrayList<DataPoint> negData = new ArrayList<>();
	double curEntropy = calculateEntropy(remainingData);
	for (int i = 0; i < usedAttrValues.size(); i++) {
	    for (String attrValue : usedAttrValues.get(i)) {
		posData.clear();
		negData.clear();
		
		for (DataPoint dataPoint : remainingData) {
		    if (dataPoint.getData().get().get(i).equals(attrValue)) {
			posData.add(dataPoint);
		    } else {
			negData.add(dataPoint);
		    }
		}
		
		double entropy = (calculateEntropy(posData)*posData.size()/remainingData.size() +
				  calculateEntropy(negData)*negData.size()/remainingData.size());
		if(entropy < minEntropy){
		    minEntropy = entropy;
		    attrIndex = i;
		    attributeValue = attrValue;
		}		
	    }
	}


	if (aboutEqual(curEntropy, minEntropy) || minEntropy > curEntropy) {
	    return new DecisionTree(mostCommonClass(remainingData));
	}
	
	posData.clear();
	negData.clear();
	
	for (DataPoint dataPoint : remainingData) {
	    if (dataPoint.getData().get().get(attrIndex).equals(attributeValue)) {
		posData.add(dataPoint);
	    } else {
		negData.add(dataPoint);
	    }
	}
	
	DecisionTree cur = new DecisionTree(attrIndex, attributeValue);
	cur.maxClass = mostCommonClass(remainingData);	
	cur.pos = constructDT(posData);
	cur.neg = constructDT(negData);

	return cur;
    }

    private static final double EPSILON = 0.000001;
    private boolean aboutEqual(double a, double b) {
	return Math.abs(a - b) < EPSILON;
    }
    
    public String mostCommonClass(List<DataPoint> remainingData) {
	double maxProp = 0;
	List<String> commonClasses = new ArrayList<>();
	
	for (Map.Entry<String, Double> prop : classProportions(remainingData).entrySet()) {
	    if (prop.getValue() > maxProp) {
		maxProp = prop.getValue();
		
		commonClasses.clear();
		commonClasses.add(prop.getKey());
	    } else if (aboutEqual(prop.getValue(), maxProp)) {
		commonClasses.add(prop.getKey());
	    }
	}

	return commonClasses.get((int)(commonClasses.size()*Math.random()));
    }

    public Map<String, Double> classProportions(List<DataPoint> remainingData) {
	Map<String, Double> proportions = new HashMap<>();

	for (DataPoint dataPoint : remainingData) {
	    String classLabel = dataPoint.getClassLabel().get();
	    if (!proportions.containsKey(classLabel)) {
		proportions.put(classLabel, 1.0/remainingData.size());
	    } else {
		proportions.put(classLabel, proportions.get(classLabel)+1.0/remainingData.size());
	    }
	}

	return proportions;
    }
    
    public double calculateEntropy(List<DataPoint> remainingData) {
	double sum = 0.0;
	for (double proportion : classProportions(remainingData).values()) {
	    sum -= proportion * Math.log(proportion);
	}

	return sum;
    }

    public String printTree(DecisionTree dt) {
	return printTree(dt, 0);
    }
    
    public String printTree(DecisionTree dt, int indent) {
	String retS = "";
	for (int i = 0; i < indent; i++) {
	    retS += "\t";
	}	
	if (dt.clazz == null) {
	    retS += "a["+dt.attributeIndex+"]="+dt.attributeValue+":\n";
	    if (dt.pos != null) {
		retS += printTree(dt.pos, indent + 1);
	    }
	    for (int i = 0; i < indent; i++) {
		retS += "\t";
	    }
	    retS += "else:\n";	    
	    if (dt.neg != null) {
		retS += printTree(dt.neg, indent + 1);
	    }
	} else {
	    retS += "Class: " + dt.clazz + "\n";
	}
	return retS;
    }
}
