package cloudsim;

import gridsim.PE;

public class PE_Cpu extends PE{
	public static final boolean FULL = false;
	
	public PE_Cpu(int id, int MIPSRating) {
		super(id, MIPSRating);
		// TODO Auto-generated constructor stub
	}

	public void setStatusFull() {
		setStatus(FULL);
    }
	
}
