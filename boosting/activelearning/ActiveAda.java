package hw4.boosting.activelearning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

public class ActiveAda {

	static int len=4601;
	static int featlen=57;
	static int classno=2;
	static int trainlen,testlen,restlen;
	static int stop=200;
	static double[][] features = new double[len][featlen];
	static double[] labels = new double[len];
	static ArrayList<double[][]> featklist = new ArrayList<double[][]>();
	static ArrayList<double[]> labelsklist = new ArrayList<double[]>();
	static ArrayList<Integer> indexlist = new ArrayList<Integer>();
	static double[][] featurestrain = new double[1][featlen];
	static double[][] featurestrainrest = new double[1][featlen];
	static double [] labelstrain = new double[1];
	static double [] labelstrainrest = new double[1];
	static double [][] featurestest= new double[1][featlen];
	static double [] labelstest= new double[1];
	static double dataweights[];
	static ArrayList<ArrayList<Double>> stumps;
	static ArrayList<Double> alphat;
	static ArrayList<Integer> stumpt;
	static ArrayList<Double> threstt;
	static ArrayList<Double> errort;
	static ArrayList<Double> auct;
	static ArrayList<Double> trainerrort;
	static ArrayList<Double> testerrort;
	static double avgtest=0.0;
	static double avgtrain=0.0;
	static double avgre=0.0;
	static double avgtauc=0.0;
	static double totaltime=0.0;
	static double avgruns=0.0;
	static int runs;
	static int iter=0;
	static HashMap<Double, Double> allerror = new HashMap<Double, Double>(); 
	private static void readData()
	{
		int one=0,zero=0;
		try {
			FileReader featureread = new FileReader("data/spambase.data");
			BufferedReader featurereadbr = new BufferedReader(featureread);
			String sCurrentLine;
			String[] feats;
			int ind=0;
			while ((sCurrentLine = featurereadbr.readLine()) != null) {
				indexlist.add(ind);
				feats=sCurrentLine.split(",");
				for(int i=0;i<feats.length-1;i++)
				{
					features[ind][i]=Double.parseDouble(feats[i]);

				}
				labels[ind]=Double.parseDouble(feats[feats.length-1]);
				if(labels[ind]==0.0)
					zero++;
				else
					one++;
				ind++;
			}
			featurereadbr.close();
			featureread.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		//System.out.println(zero+"   "+one+"  "+(double) zero/(zero+one));
	}
	private static void randSplits(int folds)
	{
		HashMap<Integer, Boolean> seen = new HashMap<Integer, Boolean>();
		int kfold=len/folds,listno=0,one=0,zero=0;
		Random n = new Random(); 
		while(indexlist.size()!=0){
			one=0;zero=0;
			int r=Math.min(kfold, indexlist.size());
			double[][] featurestemp = new double[r][featlen];
			double [] labelstemp = new double[r];
			for (int i = 0; i < kfold; i++) {
				if(indexlist.size()==0)
					break;
				int ind=n.nextInt(indexlist.size());
				int tmpind=indexlist.get(ind);
				if(seen.containsKey(tmpind))
					System.out.println("Error: bad Coder");
				else
					seen.put(tmpind, true);
				for (int j = 0; j < featlen; j++) {
					featurestemp[i][j]=features[tmpind][j];
				}
				labelstemp[i]=labels[tmpind];
				if(labels[tmpind]==0.0)
					zero++;
				else if(labels[tmpind]==1.0)
					one++;
				else
					System.out.println("Error: bad Coder");

				indexlist.remove(ind);
			}
			//System.out.println(zero+"   "+one+"   "+(double) zero/(zero+one));
			featklist.add(listno,featurestemp);
			labelsklist.add(listno,labelstemp);
			listno++;
		}
	}
	private static void uniformSplits(int folds){
		ArrayList<ArrayList<double[]>> tft= new ArrayList<ArrayList<double[]>>();
		ArrayList<ArrayList<Double>> tlt= new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < folds; i++) {
			tft.add(new ArrayList<double[]>());
			tlt.add(new ArrayList<Double>());
		}
		for (int i = 0; i < len; i++) {
			ArrayList<double[]> temp = tft.get(i%folds);
			temp.add(features[i]);
			tft.set(i%folds, temp);
			ArrayList<Double> lemp = tlt.get(i%folds);
			lemp.add(labels[i]);
			tlt.set(i%folds,lemp);
		}
		if(tft.size()!=tlt.size())
			System.out.println("Error: BadCode");
		int one=0,zero=0,listno=0;
		for (int i = 0; i < tft.size(); i++) {
			one=0;zero=0;
			double[][] featurestemp = new double[tft.get(i).size()][featlen];
			double [] labelstemp = new double[tlt.get(i).size()];
			for (int j = 0; j < tft.get(i).size(); j++) {
				featurestemp[j]=tft.get(i).get(j);
				labelstemp[j]=tlt.get(i).get(j);
				if(labelstemp[j]==0)
					zero++;
				else
					one++;
			}
			//System.out.println(zero+"   "+one+"   "+(double) zero/(zero+one));
			featklist.add(listno,featurestemp);
			labelsklist.add(listno,labelstemp);
			listno++;
		}
	}
	private static void pickKfold(int k){
		k--;
		testlen=labelsklist.get(k).length;
		trainlen=len-testlen;
		featurestrain=new double[trainlen][featlen];
		labelstrain=new double[trainlen];
		featurestest=new double[testlen][featlen];
		labelstest=new double[testlen];
		int ind=0;
		for (int i = 0; i < featklist.size(); i++) {
			if(i==k)
			{
				featurestest=featklist.get(k);
				labelstest=labelsklist.get(k);
				continue;
			}
			double[][] tf = featklist.get(i);
			double[] tl = labelsklist.get(i);
			for (int j = 0; j < tl.length; j++) {
				featurestrain[ind]=tf[j];
				labelstrain[ind]=tl[j];
				ind++;
			}
		}
	}
	private static void pickKfold(int k,int train,int test){
		k--;
		//int onelength=labelsklist.get(k).length;
		testlen=(len*test)/100;
		trainlen=(len*train)/100;
		restlen=len-testlen-trainlen;
		featurestrain=new double[trainlen][featlen];
		labelstrain=new double[trainlen];
		featurestrainrest=new double[len-trainlen-testlen][featlen];
		labelstrainrest=new double[len-trainlen-testlen];
		featurestest=new double[testlen][featlen];
		labelstest=new double[testlen];
		int ind=0,indtest=0,indrest=0;
		for (int i = 0; i < featklist.size(); i++) {
			double[][] tf = featklist.get(i);
			double[] tl = labelsklist.get(i);
			if(i==k)
			{
				for (int j = 0; j < tl.length; j++) {
					if(indtest<testlen){
						featurestest[indtest]=tf[j];
						labelstest[indtest]=tl[j];
						indtest++;
					}
					else if(ind<trainlen){
						featurestrain[ind]=tf[j];
						labelstrain[ind]=tl[j];
						ind++;
					}
					else
					{
						featurestrainrest[indrest]=tf[j];
						labelstrainrest[indrest]=tl[j];
						indrest++;
					}
				}
			}
			else
			{
				for (int j = 0; j < tl.length; j++) {
					if(ind<trainlen){
						featurestrain[ind]=tf[j];
						labelstrain[ind]=tl[j];
						ind++;
					}
					else if(indtest<testlen){
						featurestest[indtest]=tf[j];
						labelstest[indtest]=tl[j];
						indtest++;
					}
					else
					{
						featurestrainrest[indrest]=tf[j];
						labelstrainrest[indrest]=tl[j];
						indrest++;
					}
				}
			}
		}
	}
	private static void setDataweights(){
		dataweights=new double[trainlen] ;
		for (int i = 0; i < trainlen; i++) {
			dataweights[i]=1.0/(double) trainlen;
		}
	}
	private static void createStumps(){
		for (int i = 0; i < featlen; i++) {
			ArrayList<Double> featstump=new ArrayList<Double>();
			TreeSet<Double> enefeat = new TreeSet<Double>();
			for (int j = 0; j < trainlen; j++) {
				enefeat.add(featurestrain[j][i]);
			}
			boolean first=true;
			double preval=0.0;
			double thresh=0.0;
			double avgthresh=0.0;
			for (Double tval : enefeat) {
				if(first)
				{
					preval=tval;
					first=false;
					continue;
				}
				thresh=(tval-preval)/2.0;
				preval=tval;
				avgthresh+=thresh;
				featstump.add(thresh);
			}
			avgthresh/=(double) (enefeat.size()-1);
			featstump.add(0,enefeat.last()+avgthresh);
			featstump.add(enefeat.first()+avgthresh);
			stumps.add(i, featstump);
		}
	}
	private static void init(){
		stumps=new ArrayList<ArrayList<Double>>();
		alphat=new ArrayList<Double>();
		stumpt=new ArrayList<Integer>();
		threstt=new ArrayList<Double>();
		errort=new ArrayList<Double>();
		auct=new ArrayList<Double>();
		trainerrort=new ArrayList<Double>();
		testerrort=new ArrayList<Double>();
		runs =0;
		setDataweights();
		createStumps();
	}
	private static double getStumperror(int feat,double thresh){
		double error=0.0;
		for (int i = 0; i < trainlen; i++) {
			if(featurestrain[i][feat]>thresh)
			{
				if(labelstrain[i]==0)
				{
					error+=dataweights[i];
				}
			}
			else
			{
				if(labelstrain[i]==1)
				{
					error+=dataweights[i];
				}
			}
		}
		return error;
	}
	private static double[] getRandStump(){
		Random n = new Random();
		int stumpno=0,threshno=0;
		while(true){
			stumpno=n.nextInt(stumps.size());
			if(stumps.get(stumpno).size()!=0)
			{ 
				threshno=n.nextInt(stumps.get(stumpno).size());
				break;
			}
		}
		double thresh=stumps.get(stumpno).get(threshno);
		double reverror=getStumperror(stumpno, thresh);
		ArrayList<Double> temp = stumps.get(stumpno);
		temp.remove(thresh);
		stumps.set(stumpno, temp);
		double ans[]={stumpno,thresh,reverror};
		return ans;
	}
	private static double[] getBestStump(){
		double error=0.0,temperror=0.0,reverror=0.0;
		int stumpno=0;
		double thresh=0.0;
		for (int i = 0; i < featlen; i++) {
			ArrayList<Double> featsump = stumps.get(i);
			for (int j = 0; j < featsump.size(); j++) {
				temperror=getStumperror(i, featsump.get(j));
				if(Math.abs(0.5-temperror)>error)
				{
					reverror=temperror;
					error=Math.abs(0.5-temperror);
					stumpno=i;
					thresh=featsump.get(j);
				}
			}
		}
		ArrayList<Double> temp = stumps.get(stumpno);
		temp.remove(thresh);
		stumps.set(stumpno, temp);
		double ans[]={stumpno,thresh,reverror};
		return ans;
	}
	private static void oneRun(boolean mode){
		double stump[]=new double[3];
		if(mode)
			stump=getBestStump();
		else
			stump=getRandStump();
		double alpha=0.5*Math.log((1.0-stump[2])/(stump[2]));
		alphat.add(alpha);
		stumpt.add((int) stump[0]);
		threstt.add(stump[1]);
		errort.add(stump[2]);
		double zt=0.0;
		for (int i = 0; i < trainlen; i++) {
			if(featurestrain[i][(int) stump[0]]>stump[1])
			{
				if(labelstrain[i]==0)
				{
					dataweights[i]*=Math.exp(alpha);
				}
				else
				{
					dataweights[i]*=Math.exp(-alpha);
				}
			}
			else
			{
				if(labelstrain[i]==1)
				{
					dataweights[i]*=Math.exp(alpha);
				}
				else
				{
					dataweights[i]*=Math.exp(-alpha);
				}
			}
			zt+=dataweights[i];
		}
		for (int i = 0; i < trainlen; i++) {
			dataweights[i]/=zt;
		}
		runs++;
	}
	private static double[] prediction(double[][]featureseval,double[] labelseval,int tetrlen){
		double[] predicted=new double[tetrlen];
		for (int i = 0; i < tetrlen; i++) {
			predicted[i]=0.0;
			for (int j = 0; j < runs; j++) {
				if(featureseval[i][stumpt.get(j)]>threstt.get(j))
				{
					predicted[i]+=alphat.get(j);
				}
			}
		}
		return predicted;
	}
	private static void getResult(double[][]featureseval,double[] labelseval,int tetrlen,boolean auc,boolean write){
		double prediction[]=prediction(featureseval, labelseval, tetrlen);
		TreeMap<Double, Integer[]> allpredict=new TreeMap<Double, Integer[]>();
		double thresh=0.5;
		double error=tetrlen;
		int totalone=0;
		for (int i = 0; i < tetrlen; i++) {
			Integer[] oz={0,0};
			if(allpredict.containsKey(prediction[i]))
				oz=allpredict.get(prediction[i]);
			if(labelseval[i]==0)
				oz[0]++;
			else
				oz[1]++;
			allpredict.put(prediction[i], oz);
			totalone+=labelseval[i];
		}
		int iter=0,tp=totalone,fp=tetrlen-totalone,tn=0,fn=0;
		double y1=((double)tp)/((double)tp+fn),x2=0.0,x1=((double)fp)/((double)fp+tn),y2=0.0,aucval=0.0;
		ArrayList<Integer[]> ccall=new ArrayList<Integer[]>();
		double itererror=0.0,preval=0.0;
		itererror=fp+fn;
		itererror/=(double) tetrlen;
		if(itererror<error)
		{
			error=itererror;
			//thresh=allpredict.firstKey()-0.01;
		}
		if(write)
		{
			Integer[] cctemp={iter,tp,fp,tn,fn};
			ccall.add(cctemp);
		}
		for(Double key:allpredict.keySet()){
			iter++;
			Integer[] oz = allpredict.get(key);
			tp-=oz[1];
			fn+=oz[1];
			fp-=oz[0];
			tn+=oz[0];
			if(write)
			{
				Integer[] cctemp={iter,tp,fp,tn,fn};
				ccall.add(cctemp);
			}
			itererror=fp+fn;
			itererror/=(double) tetrlen;
			if(itererror<error)
			{
				error=itererror;
				//thresh=iterthresh;
			}
			if(auc)
			{
				y2=((double)tp)/((double)tp+fn);
				x2=((double)fp)/((double)fp+tn);
				aucval+=(0.5*(y1+y2)*(x1-x2));
				x1=x2;
				y1=y2;
			}
		}
		iter++;tp=0;fp=0;tn=tetrlen-totalone;fn=totalone;
		if(write)
		{
			Integer[] cctemp={iter,tp,fp,tn,fn};
			ccall.add(cctemp);
		}
		itererror=fp+fn;
		itererror/=(double) tetrlen;
		if(itererror<error)
		{
			error=itererror;
			//thresh=iterthresh;
		}
		if(auc)
		{
			y2=((double)tp)/((double)tp+fn);
			x2=((double)fp)/((double)fp+tn);
			aucval+=(0.5*(y1+y2)*(x1-x2));
			testerrort.add(error);
			auct.add(aucval);
		}
		if(!auc)
		{
			trainerrort.add(error);
		}
		if(write)
		{
			try {
				FileWriter fw = new FileWriter("resulthw4/roc");
				BufferedWriter bw = new BufferedWriter(fw);
				for (int i = 0; i < ccall.size(); i++) {
					Integer[] cc = ccall.get(i);
					bw.write(cc[0]+" "+cc[1]+" "+cc[2]+" "+cc[3]+" "+cc[4]+"\n");
				}
				bw.close();
				fw.close();

			}catch (Exception e){
				System.out.println(e.getMessage());	
			}
		}
	}
	private static void allput(boolean mode,boolean graph){
		init();
		long startfold = System.currentTimeMillis( );
		double preverror=1.0,convthresh=0.03,seconv=0.04;
		boolean converged=false;
		while(!converged){
			oneRun(mode);
			getResult(featurestrain, labelstrain, trainlen, false,false);
			getResult(featurestest, labelstest, testlen, true,false);
			if(runs>stop)
				converged=true;
		}
		//System.out.println("Aplha: "+alphat.get(runs-1));
		System.out.println("Iteration No: "+(runs)+" Feature No: "+(stumpt.get(runs-1)+1)+" Threshold: "+threstt.get(runs-1)+" Rounderror: "+errort.get(runs-1)+" Trainerror: "+trainerrort.get(runs-1)+" Testerror: "+testerrort.get(runs-1)+" AUC: "+auct.get(runs-1));
		long endfold = System.currentTimeMillis( );
		System.out.println("Process took "+(endfold-startfold)/1000+" secs");
		allerror.put(100*((double) trainlen/(double) len), testerrort.get(runs-1));
	}
	private static void actualactive(boolean mode){
		while(trainlen<=len/2){
			allput(true,false);
			iter++;
			double[] p = prediction(featurestrainrest, labelstrainrest, restlen);
			TreeMap<Double, Integer> sort=new TreeMap<Double, Integer>();
			for (int i = 0; i < p.length; i++) {
				sort.put(Math.abs(p[i]-Math.signum(p[i])), i);
				//sort.put(-p[i], i);
			}
			double twopercent=(len*3)/100;
			HashMap<Integer, Boolean> off = new HashMap<Integer, Boolean>();
			int tp=0;
			for (Double key:sort.keySet()) {
				if(tp==twopercent)
					break;
				off.put(sort.get(key), false);
				tp++;
			}
			double temptrain[][]=new double[(int) (trainlen+twopercent)][featlen];
			double temprest[][]=new double[(int) (restlen-twopercent)][featlen];
			double temptrainlab[]=new double[(int) (trainlen+twopercent)];
			double temprestlab[]=new double[(int) (restlen-twopercent)];

			int restind=0,trainind=0;
			for (int i = 0; i < restlen; i++) {
				if(off.containsKey(i))
				{
					temptrain[trainind]=featurestrainrest[i];
					temptrainlab[trainind]=labelstrainrest[i];
					trainind++;
				}
				else
				{
					temprest[restind]=featurestrainrest[i];
					temprestlab[restind]=labelstrainrest[i];
					restind++;
				}
			}
			for (int i = 0; i < trainlen; i++) {
				temptrain[trainind]=featurestrain[i];
				temptrainlab[trainind]=labelstrain[i];
				trainind++;	
			}
			featurestrain=temptrain;
			labelstrain=temptrainlab;
			featurestrainrest=temprest;
			labelstrainrest=temprestlab;
			trainlen=trainind;
			restlen=restind;
		}
	}
	public static void main(String[] args) {
		readData();
		uniformSplits(10);
		pickKfold(1,5,20);
		actualactive(false);
		try {
			FileWriter fwte = new FileWriter("resulthw4/activeada");
			BufferedWriter bwte = new BufferedWriter(fwte);
			for (Double key:allerror.keySet()) {
			  bwte.write(key+" "+allerror.get(key)+"\n");	
			}
			bwte.close();
			fwte.close();

		}catch (Exception e){
			System.out.println(e.getMessage());	
		}
	}

}
