package eccm.dto;

import java.util.List;

import eccm.entity.EccmEquipment;

public class JobList implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private String jobId;

	private int masterSsId;
	
	private int ssId;

	private String ssName;

	private int newSsId;

	private String newSsName;

	private Integer pairNewSsId;

	private List<EccmEquipment> equipList;

	public JobList() {
	}

	public JobList(String jobId, int ssId, String ssName, int newSsId, String newSsName, Integer pairNewSsId, int masterSsId) {
		super();
		this.jobId = jobId;
		this.ssId = ssId;
		this.ssName = ssName;
		this.newSsId = newSsId;
		this.newSsName = newSsName;
		this.pairNewSsId = pairNewSsId;
		this.masterSsId = masterSsId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public int getSsId() {
		return ssId;
	}

	public void setSsId(int ssId) {
		this.ssId = ssId;
	}

	public String getSsName() {
		return ssName;
	}

	public void setSsName(String ssName) {
		this.ssName = ssName;
	}

	public int getNewSsId() {
		return newSsId;
	}

	public void setNewSsId(int newSsId) {
		this.newSsId = newSsId;
	}

	public String getNewSsName() {
		return newSsName;
	}

	public void setNewSsName(String newSsName) {
		this.newSsName = newSsName;
	}

	public List<EccmEquipment> getEquipList() {
		return equipList;
	}

	public void setEquipList(List<EccmEquipment> equipList) {
		this.equipList = equipList;
	}

	public Integer getPairNewSsId() {
		return pairNewSsId;
	}

	public void setPairNewSsId(Integer pairNewSsId) {
		this.pairNewSsId = pairNewSsId;
	}

	public int getMasterSsId() {
		return masterSsId;
	}

	public void setMasterSsId(int masterSsId) {
		this.masterSsId = masterSsId;
	}

}
