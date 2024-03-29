/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */


package cloudsim;

import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.ResGridlet;
import gridsim.ResGridletList;

import java.util.Iterator;


/**
 * TimeSharedVMScheduler implements a policy of
 * scheduling performed by a virtual machine.
 * Gridlets execute time-shared in VM.
 * 
 * @author Rodrigo N. Calheiros
 * @since CloudSim Toolkit 1.0 Beta
 * @invariant $none
 */
public class CPUSharedVMScheduler extends VMScheduler {

	protected ResGridletList gridletExecList;
	protected ResGridletList gridletPausedList;
	protected ResGridletList gridletFinishedList;
	protected int currentCPUs;
	protected double[] currentMIPSShare;
	
	/**
	 * Creates a new TimeSharedVMScheduler object. This method must be invoked
	 * before starting the actual simulation.
	 * 
	 * @pre $none
	 * @post $none 
	 */
	public CPUSharedVMScheduler() {
		super();
		this.gridletExecList = new ResGridletList();
		this.gridletPausedList = new ResGridletList();
		this.gridletFinishedList = new ResGridletList();
		this.currentCPUs=0;
	}
	
	/**
	 * Updates the processing of gridlets running under management of this scheduler.
	 * @param currentTime current simulation time
	 * @param mipsShare array with MIPS share of each processor available to the scheduler
	 * @return time predicted completion time of the earliest finishing gridlet, or 0
	 * 				if there is no next events
	 * @pre currentTime >= 0
	 * @post $none
	 */
	@SuppressWarnings("unchecked")
	@Override
	public double updateVMProcessing(double currentTime, double[]mipsShare) {

		this.currentMIPSShare=mipsShare;
		double timeSpam = currentTime-previousTime;

		double capacity = 0.0;
		int cpus=0;
		for(int i=0;i<mipsShare.length;i++){
			capacity+=mipsShare[i];
			if(mipsShare[i]>0)cpus++;
		}
		currentCPUs=cpus;

		int pesInUse = 0;
		Iterator iter = gridletExecList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();		
			pesInUse+=rgl.getNumPE();
		}
		
		if(pesInUse>currentCPUs){
			capacity/=pesInUse;
		} else {
			//System.out.println("- Capacity:"+capacity);
			capacity/=currentCPUs;
			//System.out.println("+ Capacity:"+capacity);
		}

		iter = gridletExecList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();		
			rgl.updateGridletFinishedSoFar(capacity*timeSpam*rgl.getNumPE());
		}

		double nextEvent=Double.MAX_VALUE;
		int i=0;
		
		if(gridletExecList.size()==0) {
			this.previousTime=currentTime;
			return 0.0;
		}
		
        while (i<gridletExecList.size()){
            ResGridlet obj = (ResGridlet) gridletExecList.get(i);
            double remainingLength = obj.getRemainingGridletLength();

            if (remainingLength==0.0){//finished: remove from the list
                gridletExecList.remove(obj);
                gridletFinish(obj);
                continue;
            } else {//not finish: estimate the finish time
            	double estimatedFinishTime = currentTime+(remainingLength/(capacity*obj.getNumPE()));
            	
            	if(estimatedFinishTime<nextEvent) {
            		nextEvent = estimatedFinishTime;
            	
	            	if(nextEvent-currentTime<0.01){//finish anyway, rounding issue...
	            		gridletExecList.remove(obj);
	                    gridletFinish(obj);
	                    continue;
	            	}
	            }
            }

            i++;
        }
		this.previousTime=currentTime;
		return nextEvent;
	}

	/**
	 * Cancels execution of a gridlet
	 * @param glId ID of the gridlet being cancealed
	 * @return the canceled gridlet, $null if not found
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Cloudlet cloudletCancel(int glId) {
		
		boolean found=false;
		int position=0;
		
		//First, looks in the finished queue
		found=false;
		Iterator iter = gridletFinishedList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) {
				found=true;
				break;
			}
			position++;
		}
		
		if (found){
			Cloudlet gl = (Cloudlet) gridletFinishedList.remove(position);
			
			return gl;
		}
		
		//Then searches in the exec list
		iter = gridletExecList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) {
				found=true;
				break;
			}
			position++;
		}
		
		if (found){
			
			ResGridlet rgl = (ResGridlet) gridletExecList.remove(position);
			if (rgl.getRemainingGridletLength() == 0.0) {
				gridletFinish(rgl);
			} else {
				rgl.setGridletStatus(Gridlet.CANCELED);
			}
			return (Cloudlet) rgl.getGridlet();
			
		}
		
		//Now, looks in the paused queue
		found=false;
		iter = gridletPausedList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) {
				found=true;
				rgl.setGridletStatus(Gridlet.CANCELED);
				break;
			}
			position++;
		}
		
		if (found){
			Cloudlet gl = (Cloudlet) gridletPausedList.remove(position);
			
			return gl;
		}
						
		return null;
		
	}

	/**
	 * Pauses execution of a gridlet
	 * @param glId ID of the gridlet being paused
	 * @return $true if gridlet paused, $false otherwise
	 * @pre $none
	 * @post $none
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean cloudletPause(int glId) {
		
		boolean found=false;
		int position=0;
		
		Iterator iter = gridletExecList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) {
				found=true;
				break;
			}
			position++;
		}
		
		if (found){
			
			//remove gridlet from the exec list and put it in the paused list
			ResGridlet rgl = (ResGridlet) gridletExecList.remove(position);
			if (rgl.getRemainingGridletLength() == 0.0) {
				gridletFinish(rgl);
			} else {
				rgl.setGridletStatus(Gridlet.PAUSED);
				gridletPausedList.add(rgl);
			}
			return true;
			
		}
		return false;
		
	}

	/**
	 * Processes a finished gridlet
	 * @param rgl finished gridlet
	 * @pre rgl != $null
	 * @post $none
	 */
	@SuppressWarnings("unchecked")
	private void gridletFinish(ResGridlet rgl) {
		rgl.setGridletStatus(Gridlet.SUCCESS);
        rgl.finalizeGridlet();
        gridletFinishedList.add(rgl);
	}

	/**
	 * Resumes execution of a paused gridlet
	 * @param glId ID of the gridlet being resumed
	 * @return expected finish time of the gridlet, 0.0 if queued
	 * @pre $none
	 * @post $none
	 */
	@SuppressWarnings("unchecked")
	@Override
	public double cloudletResume(int glId) {
		
		boolean found=false;
		int position=0;
		
		//look for the gridlet in the paused list
		Iterator iter = gridletPausedList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) {
				found=true;
				break;
			}
			position++;
		}
		
		if (found){
			ResGridlet rgl = (ResGridlet) gridletPausedList.remove(position);
			rgl.setGridletStatus(Gridlet.INEXEC);
			gridletExecList.add(rgl);
			
			//calculate the expected time for gridlet completion
			//first: how many PEs do we have?
			double capacity = 0.0;
			int cpus=0;
			for(int i=0;i<currentMIPSShare.length;i++){
				capacity+=currentMIPSShare[i];
				if(currentMIPSShare[i]>0)cpus++;
			}
			currentCPUs=cpus;
			
			//how many PEs each gridlet uses?
			int pesInUse = 0;
			iter = gridletExecList.iterator();
			while(iter.hasNext()){
				ResGridlet glt = (ResGridlet) iter.next();		
				pesInUse+=glt.getNumPE();
			}
			
			//available capacity for each gridlet
			if(pesInUse>currentCPUs){
				capacity/=pesInUse;
			} else {
				capacity/=currentCPUs;
			}

			double remainingLength = rgl.getRemainingGridletLength();
			double estimatedFinishTime = GridSim.clock()+(remainingLength/(capacity*rgl.getNumPE()));
        	
			return estimatedFinishTime;
		}
		
		return 0.0;
	}

	/**
	 * Receives an gridlet to be executed in the VM managed by this scheduler
	 * @param gl the submited gridlet
	 * @param fileTransferTime time required to move the required files from the SAN to the VM
	 * @return expected finish time of this gridlet
	 * @pre gl != null
	 * @post $none
	 */
	@SuppressWarnings("unchecked")
	@Override
	public double cloudletSubmit(Cloudlet gl, double fileTransferTime) {

		ResGridlet rgl = new ResGridlet(gl);
		rgl.setGridletStatus(Gridlet.INEXEC);
		for(int i=0;i<gl.getNumPE();i++){
			rgl.setMachineAndPEID(0,i);
		}
		
		double size = rgl.getGridletLength();
		size*=rgl.getNumPE();
		rgl.getGridlet().setGridletLength(size);
		gridletExecList.add(rgl);
		
		//calculate the expected time for gridlet completion
		//first: how many PEs do we have?
		double capacity = 0.0;
		int cpus=0;
		for(int i=0;i<currentMIPSShare.length;i++){
			capacity+=currentMIPSShare[i];
			//System.out.println("- Capacity:"+capacity);
			if(currentMIPSShare[i]>0)cpus++;
		}
		
		//use the current capacity to estimate the extra amount of
		//time to file transferring. It must be added to the gridlet length
		double extraSize=(capacity/cpus)*fileTransferTime;
		double length = gl.getGridletLength();
		length+=extraSize;
		gl.setGridletLength(length);
		
		currentCPUs=cpus;
		
		//how many PEs each gridlet uses?
		int pesInUse = 0;
		Iterator iter = gridletExecList.iterator();
		while(iter.hasNext()){
			ResGridlet glt = (ResGridlet) iter.next();		
			pesInUse+=glt.getNumPE();
		}
		
		//available capacity for each gridlet
		if(pesInUse>currentCPUs){
			capacity/=pesInUse;
		} else {
			//System.out.println("Capacity:"+capacity);
			capacity/=currentCPUs;
			//System.out.println("Capacity:"+capacity);
		}

		return capacity;
	}

	/**
	 * Gets the status of a gridlet
	 * @param glId ID of the gridlet
	 * @return status of the gridlet, -1 if gridlet not found
	 * @pre $none
	 * @post $none
	 */
	@Override
	public int cloudletstatus(int glId) {
		
		Iterator iter = gridletExecList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) return rgl.getGridletStatus();
		}
		iter = gridletPausedList.iterator();
		while(iter.hasNext()){
			ResGridlet rgl = (ResGridlet) iter.next();
			if (rgl.getGridletID()==glId) return rgl.getGridletStatus();
		}
		return -1;
	}

	/**
	 * Informs about completion of some gridlet in the VM managed
	 * by this scheduler
	 * @return $true if there is at least one finished gridlet; $false otherwise
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean isFinishedCloudlets() {
		
		return gridletFinishedList.size()>0; 

	}

	/**
	 * Returns the next gridlet in the finished list, $null if this list is empty
	 * @return a finished gridlet
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Cloudlet getNextFinishedCloudlet() {
		if(gridletFinishedList.size()>0){
			Cloudlet gl = (Cloudlet) ((ResGridlet) gridletFinishedList.removeFirst()).getGridlet();
			return gl;
		} else {
			return null;
		}
	}

	/**
	 * Returns the number of cloudlets runnning in the virtual machine
	 * @return number of cloudlets runnning
	 * @pre $none
	 * @post $none
	 */
	@Override
	public int runningCloudlets() {
		return this.gridletExecList.size();
	}

	/**
	 * Returns one cloudlet to migrate to another vm
	 * @return one running cloudlet
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Cloudlet migrateCloudlet() {
		Cloudlet cl = (Cloudlet) this.gridletExecList.removeFirst();		
		return cl;
	}

}
