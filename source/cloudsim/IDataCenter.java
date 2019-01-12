package cloudsim;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.datagrid.DataGridTags;
import gridsim.datagrid.File;
import gridsim.datagrid.storage.Storage;
import gridsim.net.InfoPacket;

public interface IDataCenter {
    public void body();
	public void printDebts();
}
