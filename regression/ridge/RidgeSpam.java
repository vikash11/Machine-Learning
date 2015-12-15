package hw2.regression.ridge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;

import Jama.Matrix;

public class RidgeSpam {

	public static void main(String[] args) {
		double[][] features = new double[4601][58];
		double [][] labels = new double[4601][1];
		ArrayList<Integer> indexlist = new ArrayList<Integer>();
		FileReader featureread;
		try {
			featureread = new FileReader("data/spambase.data");
			BufferedReader featurereadbr = new BufferedReader(featureread);
			String sCurrentLine;
			String[] feats;
			int ind=0;
			while ((sCurrentLine = featurereadbr.readLine()) != null) {
				indexlist.add(ind,ind);
				feats=sCurrentLine.split(",");
				for(int i=0;i<feats.length-1;i++)
				{
					features[ind][i]=Double.parseDouble(feats[i]);
				}
				features[ind][feats.length-1]=1.0;
				labels[ind][0]=Double.parseDouble(feats[feats.length-1]);
				ind++;
			}
			featurereadbr.close();
			featureread.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		ArrayList<double[][]> featklist = new ArrayList<double[][]>();
		ArrayList<double[][]> labelsklist = new ArrayList<double[][]>();
		int kfold=400;int listno=0;
		Random n = new Random(); 
		while(indexlist.size()!=0){
			double[][] featurestemp = new double[kfold][58];
			double [][] labelstemp = new double[kfold][1];
			for (int i = 0; i < kfold; i++) {
				if(indexlist.size()==0)
					break;
				int ind=n.nextInt(indexlist.size());
				for (int j = 0; j < 58; j++) {
					featurestemp[i][j]=features[ind][j];
				}
				labelstemp[i][0]=labels[ind][0];
				indexlist.remove(ind);
			}
			featklist.add(listno,featurestemp);
			labelsklist.add(listno,labelstemp);
			listno++;
		}
		int nind=0;Double msetr=0.0,msete=0.0;
		for (int i = 0; i < listno; i++) {
			nind=0;
			double[][] featurestrain = new double[4600][58];
			double [][] labelstrain = new double[4600][1];
			double[][] featurestest = new double[kfold][58];
			double [][] labelstest = new double[kfold][1];
			double [] mean = new double[58];
			double[] std= new double[58];
			for (int j = 0; j < listno; j++) {
				double[][] featurestemp = featklist.get(j);
				double [][] labelstemp = labelsklist.get(j);
				//System.out.println("before "+i+"  "+j+"  "+nind+"  "+labelsklist.get(j).size());
				if(j==i)
				{
					featurestest = featklist.get(j);
					labelstest = labelsklist.get(j);
				}
				else
				{
					for (int k = 0; k < labelstemp.length; k++) {
						labelstrain[nind][0]=labelstemp[k][0];
						for (int k2 = 0; k2 < 58; k2++) {
							featurestrain[nind][k2]=featurestemp[k][k2];
							mean[k2]+=featurestrain[nind][k2];
						}
						nind++;
					}
				}
			}
			for(int ij=0;ij<57;ij++)
			{
				mean[ij]/=nind;
			}
			for (int j = 0; j < mean.length-1; j++) {
				std[j]=0.0;
				for (int j2 = 0; j2 < nind; j2++) {
					std[j]+=(featurestrain[j2][j]-mean[j])*(featurestrain[j2][j]-mean[j]);    	
				}
				std[j]=Math.sqrt(std[j]);
			}
			for (int j = 0; j < nind; j++) {
				for (int j2 = 0; j2 < mean.length-1; j2++) {
					//featurestrain[j][j2]-=mean[j2]; 
					//featurestrain[j][j2]/=std[j2]; 
				}
			}
			for (int j = 0; j < 4601-nind; j++) {
				for (int j2 = 0; j2 < mean.length-1; j2++) {
					//featurestest[j][j2]-=mean[j2]; 
					//featurestest[j][j2]/=std[j2]; 
				}
			}
			nind=labelsklist.get(i).length;
			double gte=10000000.0,gtr=100.0,cte=0.0,ctr=0.0,lambda=0.001,thresh=0.000001,tacc=0.0,tracc=0.0,septhresh=0.5;
			while(true)
			{
				Matrix lam=Matrix.identity(58, 58).times(lambda);
				Matrix x = new Matrix(featurestrain);
				Matrix y = new Matrix(labelstrain);
				Matrix xt = x.transpose();
				Matrix mul2=xt.times(y);
				Matrix mul1=xt.times(x).plus(lam);
				mul1=mul1.inverse();
				Matrix w=mul1.times(mul2);
				//w.print(1, 6);
				//System.out.println(w.getRowDimension());
				Matrix xtest=new Matrix(featurestest);
				Matrix ytest = new Matrix(labelstest);
				Matrix mul3 = xtest.times(w);
				Matrix error= mul3.minus(ytest);
				Matrix fete=(error.transpose().times(error)).times(1);
				//fete.print(1, 6);
				tacc=0.0;
				for (int j = 0; j < mul3.getRowDimension(); j++) {
					//System.out.println(Math.floor(mul3.get(j, 0)+septhresh)+" test  "+ytest.get(j, 0));
					if( Math.floor(mul3.get(j, 0)+septhresh) == ytest.get(j, 0) )
					{
						tacc++;
					}
				}
				tacc/=(double) mul3.getRowDimension();
				tacc*=100;
				System.out.println("Iteration no:"+(i+1)+"  "+lambda);
				System.out.println("Testing error:"+fete.get(0, 0)/((double) (nind)));
				System.out.println("testing Accuracy:"+tacc);
				cte=fete.get(0, 0)/((double) (nind));
				//error.print(1, 6);
				Matrix mul4 = x.times(w);
				Matrix errortrain= mul4.minus(y);
				Matrix fet=(errortrain.transpose().times(errortrain)).times(1);
				//fet.print(1, 6);
				tracc=0.0;
				for (int j = 0; j < mul4.getRowDimension(); j++) {
					//System.out.println(Math.floor(mul3.get(0, j)+septhresh)+" train  "+ytest.get(0, j));
					if( Math.floor(mul4.get(j, 0)+septhresh) == y.get(j, 0) )
					{
						tracc++;
					}
				}
				tracc/=(double) mul4.getRowDimension();
				tracc*=100;
				System.out.println("Training error:"+fet.get(0, 0)/((double) (4601-nind)));
				System.out.println("training Accuracy:"+tracc);
				ctr=fet.get(0, 0)/((double) (4601-nind));
				ctr=fet.get(0, 0)/nind;
				if(Math.abs(cte-gte)<thresh)
					break;
				if(cte>gte)
					break;
				else
				{
					gte=cte;
					gtr=ctr;
					lambda+=1;
				}
			}
			System.out.println("Iteration no:"+(i+1));
			System.out.println("Testing error:"+gte);
			System.out.println("Training error:"+gtr);
			System.out.println("lambda :"+lambda);
			msetr+=gtr;
			msete+=gte;
			
		}
		System.out.println("Average testing error:"+(msete/(double) listno));
		System.out.println("Average training error:"+(msete/(double) listno));

	}

}
