package cloudsim.ext.datacenter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import cloudsim.DatacenterCharacteristics;
import cloudsim.DatacenterTags;
import cloudsim.VMCharacteristics;
import cloudsim.VirtualMachine;
import cloudsim.ext.Constants;
import cloudsim.ext.InternetCharacteristics;
import cloudsim.ext.InternetCloudlet;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;
import cloudsim.ext.stat.HourlyEventCounter;
import cloudsim.ext.stat.HourlyStat;
import cloudsim.ext.util.CommPath;
import cloudsim.ext.util.InternetEntitityRegistry;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_stat;
import eduni.simjava.Sim_system;
import gridsim.GridSim;
import gridsim.GridSimTags;

public interface IDatacenterController {
	public void body();
	public void createNewVm();
	public int getRegion();
	public String getDataCenterName();
	public double getTotalCost();
	public double getDataTransferCost();
	public double getVmCost();
	public HourlyEventCounter getHourlyArrival();
	public void addCloudSimEventListener(CloudSimEventListener l);
	public void fireCloudSimEvent(CloudSimEvent e);
	public void removeCloudSimEventListener(CloudSimEventListener l);
	public Map<Integer, VirtualMachineState> getVmStatesList();
	public HourlyStat getHourlyProcessingTimes();
	public Map<Integer, Integer> getVmAllocationStats();
	public int getAllRequestsProcessed();
	public String get_name();
	public Sim_stat get_stat();
}
