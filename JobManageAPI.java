/**
// Control Cables Management Tool(制御ケーブル管理ツール)
//
// (C)2019 Toshiba Energy Systems & Solutions Corporation
//    All Rights Reserved.
//
// パッケージ名：制御ケーブル管理パッケージ
// ファイル名　：作業件名管理API(JobManageAPI.java)
//
//
// [変更履歴]
//
// (初版作成)
//   設計         2020/08/01	承認：      調査：     担当：（福技）三木
//   製造             /  /     		承認：      調査：     担当：（福技）ハイ
//   単体試験      /  /     		承認：      調査：     担当：（福技）ハイ
//
// Rev.               /  /     承認：      調査：     担当：
//===============================================================================
//  Version     変更日            変更者              変更内容
//-------------------------------------------------------------------------------------------------------------------------------------------
//  1.01       2020/08/01    (TST) Hai       	- Create file
//  1.02       2022/02/18    (TSDV)TheNV       	- Add API search job has lockStep is approval (clone from method searchJob())
//-------------------------------------------------------------------------------------------------------------------------------------------
 *  Rev.q001               /  /     承認：      調査：     担当： VuQT
 *  変更理由
 *  2022/06/02         (TSDV) VuQT                東北向けの修正
*/
package eccm.api;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eccm.dto.ApiRespone;
import eccm.dto.ApiStatus;
import eccm.dto.CcmJobDTO;
import eccm.dto.JobList;
import eccm.dto.JobListData;
import eccm.dto.KeyValue;
import eccm.entity.CcmBase;
import eccm.entity.CcmCable;
import eccm.entity.CcmCore;
import eccm.entity.CcmEquipment;
import eccm.entity.CcmJob;
import eccm.entity.CcmJumper;
import eccm.entity.CcmSsConstruction;
import eccm.entity.CcmStationMaster;
import eccm.entity.CcmSysDef;
import eccm.entity.CcmTerminal;
import eccm.entity.EccmBase;
import eccm.entity.EccmCJob;
import eccm.entity.EccmCable;
import eccm.entity.EccmColMast;
import eccm.entity.EccmCore;
import eccm.entity.EccmEquipment;
import eccm.entity.EccmJob;
import eccm.entity.EccmJumper;
import eccm.entity.EccmSsConstruction;
import eccm.entity.EccmTerminal;
import eccm.entity.WorkDrawingData;
import eccm.exception.CustomException;
import eccm.tools.Utils;
import net.arnx.jsonic.JSON;

@Path("/api")
public class JobManageAPI extends BaseAPI {
	Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@PersistenceContext
	private EntityManager em;

	@Resource
	private UserTransaction userTx;

	public JobManageAPI() {
		InitialContext ctx;
		try {
			ctx = new InitialContext();
			userTx = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}

	@GET
	@Path("/job/all")
	public String getAllJobAPI() {
		log.info("GET /job/all");
		
		List<JobList> jobList = new ArrayList<>();

		try {

			jobList = em.createNamedQuery("EccmJob.findAllJobList", JobList.class).getResultList();

			for (JobList job : jobList) {

				List<EccmEquipment> equips = em.createNamedQuery("EccmEquipment.findBySsId", EccmEquipment.class)
																											.setParameter("id", job.getNewSsId())
																											.getResultList();

				if(equips != null) {
					for (EccmEquipment equipment : equips) {
						equipment.setName(equipment.getFullName());
					}
				}

				job.setEquipList(equips);
			}

		} catch(Exception e) {
			e.printStackTrace();
			return json.format(new ApiRespone(API_STATUS_NG, e.getMessage()));
		}

		return json.format(new ApiRespone(API_STATUS_OK, jobList));
	}

	

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/job/get/{jobid}")
	public String getJobById(@PathParam("jobid") String jobid) {
		log.info("/job/get/" + jobid);

		EccmJob job = new EccmJob();

		try {

			job = em.createNamedQuery("EccmJob.findById", EccmJob.class).setParameter("jobId", jobid).getSingleResult();

		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiRespone(API_STATUS_NG, e.getMessage()));
		}

		return json.format(new ApiRespone(API_STATUS_OK, job));
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/point/job/get/{ssid}")
	public String getJobList(@PathParam("ssid") int ssid) {
		log.info("/point/job/get/" + ssid);

		JobListData data = new JobListData();
		try {
			List<String> eccmJobIds = em.createNamedQuery("EccmJob.findAllJobId", String.class).getResultList();
			List<CcmJob> allJobsBySs = em.createNamedQuery("CcmJob.findAllBySsID", CcmJob.class).setParameter("ssId", ssid).getResultList();
			List<CcmJob> validJobList = new ArrayList<CcmJob>();
			for (CcmJob job : allJobsBySs) {
				if(eccmJobIds.contains(job.getJobId())) {
					validJobList.add(job);
				}
			}
			data.jobs = validJobList;
			
			data.stations = em.createNamedQuery("CcmStationMaster.findAll", CcmStationMaster.class).getResultList();
			data.constructions = em.createNamedQuery("CcmSsConstruction.findAll", CcmSsConstruction.class).getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiStatus("no data", null));
		}

		JSON parser = new JSON();
		parser.setDateFormat("yyyy-MM-dd");

		return parser.format(data);
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/point/job/get")
	public String getJobList() {
		log.info("/point/job/get");

		JobListData data = new JobListData();
		try {
			data.stations = em.createNamedQuery("CcmStationMaster.findAll", CcmStationMaster.class).getResultList();
			data.jobs = em.createNamedQuery("CcmJob.findAllActiveJob", CcmJob.class).getResultList();
			data.constructions = em.createNamedQuery("CcmSsConstruction.findAll", CcmSsConstruction.class).getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiStatus("no data", null));
		}

		JSON parser = new JSON();
		parser.setDateFormat("yyyy-MM-dd");

		return parser.format(data);
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/point/job/all")
	public String getEccmJobList() {
		log.info("/point/job/all");

		List<JobList> jobList = new ArrayList<>();
		try {

			jobList = em.createNamedQuery("EccmJob.findAllJobList", JobList.class).getResultList();

		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiStatus("no data", null));
		}

		JSON parser = new JSON();
		parser.setDateFormat("yyyy-MM-dd");

		return parser.format(jobList);
	}

	@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/point/job/search")
    public String searchJob(CcmJobDTO searchData) {
        log.info("/point/job/search");

        JobListData data = new JobListData();
        try {
            Map<String, Object> params = new WeakHashMap<>();
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select s from CcmJob s where 1=1 and s.ssConstId <> 0 ");

            if (searchData.getMasterSsId() != null && searchData.getMasterSsId() > 0) {
                sqlBuilder.append(" and s.masterSsId =:masterSsId ");
                params.put("masterSsId", searchData.getMasterSsId());
            }

            if (searchData.getType() != null && searchData.getType() > 0) {
                sqlBuilder.append(" and s.jobIndex1 =:type ");
                params.put("type", searchData.getType());
            }

            if (searchData.getGroup() != null && searchData.getGroup() > 0) {
                sqlBuilder.append(" and s.group =:group ");
                params.put("group", searchData.getGroup());
            }

            if (searchData.getStaff() != null && !searchData.getStaff().equals("")) {
                sqlBuilder.append(" and s.name  like :name ");
                params.put("name", "%" + searchData.getStaff() + "%");
            }

            if (searchData.getIsActiveJob() != null && searchData.getIsActiveJob() == 1) {
                sqlBuilder.append(" and s.jobStatus <> 3 ");
            }
            
            if (searchData.getYear() != null && !searchData.getYear().equals("")) {

                if (searchData.getMonth() != null && !searchData.getMonth().equals("")) {
                    String dateStart = searchData.getYear() + searchData.getMonth() + "01";
                    String dateEnd = searchData.getYear() + searchData.getMonth() + "31";

                    sqlBuilder.append(" and (( s.jobstYmdPlan  <= :dateEnd and s.jobedYmdPlan >= :dateStart ) ");
                    sqlBuilder.append(" or ( s.jobstYmdResult  <= :dateEnd and s.jobedYmdResult >= :dateStart ) ) ");

                    params.put("dateStart", dateStart);
                    params.put("dateEnd", dateEnd);
                } else if ((searchData.getMonth() == null || searchData.getMonth().equals(""))) {
                    Integer pYear = Integer.parseInt(searchData.getYear()) - 1;

                    String startYear = String.valueOf(pYear) + "04";
                    startYear = StringUtils.leftPad(startYear, 6, '0');
                    String endYear = searchData.getYear() + "03";
                    endYear = StringUtils.leftPad(endYear, 6, '0');
                    sqlBuilder.append(" and (( SUBSTRING(s.jobstYmdPlan,1,6)  <= :endYear and SUBSTRING(s.jobedYmdPlan,1,6) >= :startYear ) ");
                    sqlBuilder.append(" or ( SUBSTRING(s.jobstYmdResult,1,6)  <= :endYear and SUBSTRING(s.jobedYmdResult,1,6) >= :startYear ) ) ");

                    params.put("startYear", startYear);
                    params.put("endYear", endYear);
                }
            }

            Query query = em.createQuery(sqlBuilder.toString(), CcmJob.class);
            if (params.size() > 0) {
                for (String key : params.keySet()) {
                    query.setParameter(key, params.get(key));
                }
            }

            data.jobs = query.getResultList();
            if (data.jobs != null && data.jobs.size() > 0) {
                // add parent job id if parent not include filter condition
                List<String> jobUperIds = new ArrayList<>();
                List<String> jobParentsId = new ArrayList<>();
                for (CcmJob j : data.jobs) {
                    if (j.getJobType() == 1) {
                        jobParentsId.add(j.getJobId());
                    }
                }

                for (CcmJob j : data.jobs) {
                    if (j.getJobType() == 2) {
                        if (!jobParentsId.contains(j.getUperJobId()) && j.getUperJobId() != null) {
                            jobUperIds.add(j.getUperJobId());
                        }
                    }
                }

                if (jobUperIds.size() > 0) {
                    StringBuilder addSql = new StringBuilder("select s from CcmJob s where 1=1 ");
                    addSql.append(" and  s.jobId in (:jobId) ");
                    query = em.createQuery(addSql.toString(), CcmJob.class).setParameter("jobId", jobUperIds);
                    data.jobs.addAll(query.getResultList());
                }

            }

            // Add stations and constructions list
            data.stations = em.createNamedQuery("CcmStationMaster.findAll", CcmStationMaster.class).getResultList();
            data.constructions = em.createNamedQuery("CcmSsConstruction.findAll", CcmSsConstruction.class).getResultList();
            
        } catch (Exception e) {
            e.printStackTrace();
            return json.format(new ApiStatus("no data", null));
        }

        JSON parser = new JSON();
        parser.setDateFormat("yyyy-MM-dd");

        return parser.format(data);
    }
	// Rev.q001T-S
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/point/job/eccmsearch")
	public String searchECCMJob(CcmJobDTO searchData) {
		log.info("/point/job/eccmsearch");

		JobListData data = new JobListData();
		try {
			Map<String, Object> params = new WeakHashMap<>();
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("select s from EccmCJob s where 1=1 and s.ssConstId <> 0 "); // Rev.q001H

			if (searchData.getMasterSsId() != null && searchData.getMasterSsId() > 0) {
				sqlBuilder.append(" and s.masterSsId =:masterSsId ");
				params.put("masterSsId", searchData.getMasterSsId());
			} 
			// Rev.q001T-S
			else {
			    if (searchData.getListMasterSsId() != null && searchData.getListMasterSsId().size() > 0) {
			            List<Integer> listStationIds = searchData.getListMasterSsId();
                        //sqlBuilder.append(" and s.masterSsId in ':masterSsIds'");
                        String masterSsIds = listStationIds.stream()
                                .map(n -> String.valueOf(n))
                                .collect(Collectors.joining(",", "(", ")"));
                        //params.put("masterSsIds", masterSsIds);        
                        sqlBuilder.append(" and s.masterSsId in " + masterSsIds);
                }
			}
			// Rev.q001T-E

			if (searchData.getType() != null && searchData.getType() > 0) {
				sqlBuilder.append(" and s.jobIndex1 =:type ");
				params.put("type", searchData.getType());
			}

			if (searchData.getGroup() != null && searchData.getGroup() > 0) {
				sqlBuilder.append(" and s.group =:group ");
				params.put("group", searchData.getGroup());
			}

			if (searchData.getStaff() != null && !searchData.getStaff().equals("")) {
				sqlBuilder.append(" and s.name  like :name ");
				params.put("name", "%" + searchData.getStaff() + "%");
			}

			if (searchData.getIsActiveJob() != null && searchData.getIsActiveJob() == 1) {
				sqlBuilder.append(" and s.jobStatus <> 3 ");
			}
			
			if (searchData.getYear() != null && !searchData.getYear().equals("")) {

				if (searchData.getMonth() != null && !searchData.getMonth().equals("")) {
					String dateStart = searchData.getYear() + searchData.getMonth() + "01";
					String dateEnd = searchData.getYear() + searchData.getMonth() + "31";

					sqlBuilder.append(" and (( s.jobstYmdPlan  <= :dateEnd and s.jobedYmdPlan >= :dateStart ) ");
					sqlBuilder.append(" or ( s.jobstYmdResult  <= :dateEnd and s.jobedYmdResult >= :dateStart ) ) ");

					params.put("dateStart", dateStart);
					params.put("dateEnd", dateEnd);
				} else if ((searchData.getMonth() == null || searchData.getMonth().equals(""))) {
					Integer pYear = Integer.parseInt(searchData.getYear()) - 1;

					String startYear = String.valueOf(pYear) + "04";
					startYear = StringUtils.leftPad(startYear, 6, '0');
					String endYear = searchData.getYear() + "03";
					endYear = StringUtils.leftPad(endYear, 6, '0');
					sqlBuilder.append(" and (( SUBSTRING(s.jobstYmdPlan,1,6)  <= :endYear and SUBSTRING(s.jobedYmdPlan,1,6) >= :startYear ) ");
					sqlBuilder.append(" or ( SUBSTRING(s.jobstYmdResult,1,6)  <= :endYear and SUBSTRING(s.jobedYmdResult,1,6) >= :startYear ) ) ");

					params.put("startYear", startYear);
					params.put("endYear", endYear);
				}
			}

			Query query = em.createQuery(sqlBuilder.toString(), EccmCJob.class); // Rev.q001H
			if (params.size() > 0) {
				for (String key : params.keySet()) {
					query.setParameter(key, params.get(key));
				}
			}

			data.eccmcjobs = query.getResultList(); // Rev.q001H
			if (data.eccmcjobs != null && data.eccmcjobs.size() > 0) { // Rev.q001H
				// add parent job id if parent not include filter condition
				List<String> jobUperIds = new ArrayList<>();
				List<String> jobParentsId = new ArrayList<>();
				for (EccmCJob j : data.eccmcjobs) { // Rev.q001H
					if (j.getJobType() == 1) {
						jobParentsId.add(j.getJobId());
					}
				}

				for (EccmCJob j : data.eccmcjobs) { // Rev.q001H
					if (j.getJobType() == 2) {
						if (!jobParentsId.contains(j.getUperJobId()) && j.getUperJobId() != null) {
							jobUperIds.add(j.getUperJobId());
						}
					}
				}

				if (jobUperIds.size() > 0) {
					StringBuilder addSql = new StringBuilder("select s from EccmCJob s where 1=1 ");
					addSql.append(" and  s.jobId in (:jobId) ");
					query = em.createQuery(addSql.toString(), EccmCJob.class).setParameter("jobId", jobUperIds); // Rev.q001H
					data.eccmcjobs.addAll(query.getResultList());
				}

			}

			// Add stations and constructions list
			data.stations = em.createNamedQuery("CcmStationMaster.findAll", CcmStationMaster.class).getResultList();
			data.constructions = em.createNamedQuery("CcmSsConstruction.findAll", CcmSsConstruction.class).getResultList();
			
		} catch (Exception e) {
			//e.printStackTrace(); // Rev.q001D
			//return json.format(new ApiStatus("no data", null)); // Rev.q001D
		    throw new CustomException(e, getLoggedInUser());
		}

		JSON parser = new JSON();
		parser.setDateFormat("yyyy-MM-dd");

		return parser.format(data);
	}
	// Rev.q001T-E
	
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/job/generateJobNo")
	public String generateJobNo() {
		try {
			List<CcmSysDef> sysdefs = em.createNamedQuery("CcmSysDef.findAll", CcmSysDef.class).getResultList();
			String jobId = em.createNamedQuery("CcmJob.findJobByNendo", String.class).setParameter("nendo", "%" + sysdefs.get(0).getNendo() + "%").getSingleResult();
			String newJobId = null;
			if (jobId == null) {
				newJobId = sysdefs.get(0).getNendo() + "01";
			} else {
				String seq = jobId.substring(4, 6);
				int id = (Integer.valueOf(seq) + 1);
				if (id < 10) {
					newJobId = sysdefs.get(0).getNendo() + "0" + String.valueOf(id);
				} else {
					newJobId = sysdefs.get(0).getNendo() + String.valueOf(id);
				}

			}
			return json.format(new ApiRespone(API_STATUS_OK, newJobId));
		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiRespone(API_STATUS_NG, e.getMessage()));
		}

	}

	@POST
	@Path("/job/edit")
	@Consumes("application/json")
	@Transactional
	public String updateJobs(CcmJob job) {
		log.info("/job/edit/" + job.getJobId());
		try {
			CcmJob oldJob = em.find(CcmJob.class, job.getJobId());
			String upName = getLoggedInUser();
			Timestamp upDay = eccm.tools.Utils.getCurrentJPTime();
			if (oldJob == null) {
				job.setUpDay(upDay);
				job.setUpName(upName);
				em.persist(job);
				
				// Create new job -> copy construction from ccm_ss_const to eccm_ss_const and related data
				createJobData(job.getSsConstId(), job.getJobId());
				
				return json.format(new ApiRespone(API_STATUS_OK, "OK"));
			} else {
				oldJob.setJobName(job.getJobName());
				oldJob.setOperationYmd(job.getOperationYmd());
				oldJob.setJobIndex1(job.getJobIndex1());
				oldJob.setJobIndex2(job.getJobIndex2());
				oldJob.setJobstYmdPlan(job.getJobstYmdPlan());
				oldJob.setJobedYmdPlan(job.getJobedYmdPlan());
				oldJob.setJobstYmdResult(job.getJobstYmdResult());
				oldJob.setJobedYmdResult(job.getJobedYmdResult());
				oldJob.setPoweroutFlg(job.getPoweroutFlg());
				oldJob.setOperationFlg(job.getOperationFlg());
				oldJob.setJobStatus(job.getJobStatus());
				oldJob.setGroup(job.getGroup());
				oldJob.setName(job.getName());
				oldJob.setComp(job.getComp());
				oldJob.setJobType(job.getJobType());
				oldJob.setUperJobId(job.getUperJobId());
				em.persist(oldJob);
				return json.format(new ApiRespone(API_STATUS_OK, "OK"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiRespone(API_STATUS_NG, e.getMessage()));
		}

	}

	/**
	 * Copy construction
	 * @param scid construction id in CCM
	 * @param jobid
	 * @return
	 */
	@Transactional
	public boolean createJobData(int scid, String jobid) {

		EccmJob job = em.find(EccmJob.class, jobid);
		if(job != null) {
			return false;
		}

		CcmSsConstruction sc = em.find(CcmSsConstruction.class, scid);

		// Store equipment and terminal to mapping core, layout symbol (>= 2 foreign
		// keys), key is original id, value is clone value,
		HashMap<Integer, Integer> mapEqp = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> mapBase = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> mapTerminal = new HashMap<Integer, Integer>();

		// Clone construction
		EccmSsConstruction scCopy = new EccmSsConstruction();
		scCopy.setMasterId(sc.getMasterId());
		scCopy.setConstDetail(sc.getConstDetail());
		scCopy.setMasterStatus(sc.getMasterStatus());
		scCopy.setMasterSetDate(sc.getMasterSetDate());
		scCopy.setMakeDate(sc.getMakeDate());
		scCopy.setCompletionDate(sc.getCompletionDate());
		scCopy.setApprovalDate(sc.getApprovalDate());
		scCopy.setConstApprovalDate(sc.getConstApprovalDate());
		scCopy.setMakeName(sc.getMakeName());
		scCopy.setInuseFlg(sc.getInuseFlg());
		String upName = getLoggedInUser();
		Timestamp upDay = Utils.getCurrentJPTime();
		scCopy.setUpDay(upDay);
		scCopy.setUpName(upName);

		em.persist(scCopy);
		// End clone construction

		// Clone equipment
		List<CcmEquipment> equipList = em.createNamedQuery("CcmEquipment.findBySS", CcmEquipment.class)
				.setParameter("id", sc.getId()).getResultList();
		for (CcmEquipment equip : equipList) {

			EccmEquipment eqpCopy = new EccmEquipment();
			eqpCopy.setSsId(scCopy.getId());
			eqpCopy.setCd(equip.getCd());
			eqpCopy.setName(equip.getName());
			eqpCopy.setWork(null);

			em.persist(eqpCopy);
			mapEqp.put(equip.getId(), eqpCopy.getId());

			// Start clone base
			List<CcmBase> baseList = em.createNamedQuery("CcmBase.findByEquip", CcmBase.class)
					.setParameter("id", equip.getId()).getResultList();
			for (CcmBase base : baseList) {

				EccmBase baseCopy = new EccmBase();
				baseCopy.setEqpID(eqpCopy.getId());
				baseCopy.setCd(base.getCd());
				baseCopy.setName(base.getName());
				baseCopy.setTermCnt(base.getTermCnt());
				baseCopy.setSortType(base.getSortType());

				em.persist(baseCopy);
				mapBase.put(base.getId(), baseCopy.getId());

				// Start clone terminal
				List<CcmTerminal> terminalList = em.createNamedQuery("CcmTerminal.findByBase", CcmTerminal.class)
						.setParameter("id", base.getId()).getResultList();

				for (CcmTerminal terminal : terminalList) {
					EccmTerminal terminalCopy = new EccmTerminal();
					terminalCopy.setBaseID(baseCopy.getId());
					terminalCopy.setNum(terminal.getNum());
					terminalCopy.setCd(terminal.getCd());
					terminalCopy.setClassify(terminal.getClassify());
					terminalCopy.setConnectNum(terminal.getConnectNum());
					terminalCopy.setYobi1(terminal.getYobi1());
					terminalCopy.setYobi2(terminal.getYobi2());
					terminalCopy.setUse(terminal.getUse());

					em.persist(terminalCopy);
					mapTerminal.put(terminal.getId(), terminalCopy.getId());
				}
			}

		}

		// Start clone cable
		List<CcmCable> cableList = em.createNamedQuery("CcmCable.findBySSID", CcmCable.class)
				.setParameter("id", sc.getId()).getResultList();
		for (CcmCable cable : cableList) {
			EccmCable cableCopy = new EccmCable();
			cableCopy.setSsId(scCopy.getId());
			cableCopy.setCableKey(cable.getCableKey());
			cableCopy.setSeq(cable.getSeq());
			cableCopy.setFromCd(cable.getFromCd());
			cableCopy.setToCd(cable.getToCd());
			cableCopy.setCableLength(cable.getCableLength());
			cableCopy.setCableSize(cable.getCableSize());
			cableCopy.setCoreNum(cable.getCoreNum());
			cableCopy.setLayingYmd(cable.getLayingYmd());
			cableCopy.setCableType(cable.getCableType());
			cableCopy.setFromSpare(cable.getFromSpare());
			cableCopy.setToSpare(cable.getToSpare());
			cableCopy.setFromSNo(cable.getFromSNo());
			cableCopy.setToSNo(cable.getToSNo());

			cableCopy.setFromBaseId(mapBase.get(cable.getFromBaseId()));
			cableCopy.setToBaseId(mapBase.get(cable.getToBaseId()));

			cableCopy.setStatus(InstallationDrawingAPI.CABLE_STATUS_DEFAULT);
			cableCopy.setDelFlag(InstallationDrawingAPI.CABLE_FLAG_DEFAULT);

			em.persist(cableCopy);

			List<CcmCore> coreList = em.createNamedQuery("CcmCore.findByCableId", CcmCore.class)
					.setParameter("cableId", cable.getId()).getResultList();
			for (CcmCore core : coreList) {
				EccmCore coreCopy = new EccmCore();
				coreCopy.setCableId(cableCopy.getId());
				coreCopy.setFromBaseId(mapBase.get(core.getFromBaseId()));
				coreCopy.setToBaseId(mapBase.get(core.getToBaseId()));
				coreCopy.setFromTerminalId(mapTerminal.get(core.getFromTerminalId()));
				if (core.getToTerminalId() == 0) {
					coreCopy.setToTerminalId(0);
				} else {
					coreCopy.setToTerminalId(mapTerminal.get(core.getToTerminalId()));
				}

				coreCopy.setColorId(core.getColorId());
				coreCopy.setUse(core.getUse());
				coreCopy.setFromCableIndex(core.getFromCableIndex());
				coreCopy.setToCableIndex(core.getToCableIndex());

				em.persist(coreCopy);
			}
		}

		// Copy jumper
		List<CcmJumper> jumperList = em.createNamedQuery("CcmJumper.findBySsId", CcmJumper.class)
				.setParameter("ssId", sc.getId())
				.getResultList();
		for (CcmJumper jumper : jumperList) {
			EccmJumper jumperCopy = new EccmJumper();
			jumperCopy.setSsId(scCopy.getId());
			jumperCopy.setCableKey(jumper.getCableKey());
			jumperCopy.setSeq(jumper.getSeq());
			jumperCopy.setColorId(jumper.getColorId());
			jumperCopy.setSysNo(jumper.getSno());
			jumperCopy.setFromCableIndex(jumper.getFromCableIndex());
			jumperCopy.setToCableIndex(jumper.getToCableIndex());

			jumperCopy.setFromBaseId(mapBase.get(jumper.getFromBaseId()));
			if(jumper.getToBaseId() != 0 ) {
				jumperCopy.setToBaseId(mapBase.get(jumper.getToBaseId()));
			} else {
				jumperCopy.setToBaseId(0);
			}

			jumperCopy.setFromTerminalId(mapTerminal.get(jumper.getFromTerminalId()));
			if (jumper.getToTerminalId() == 0) {
				jumperCopy.setToTerminalId(0);
			} else {
				jumperCopy.setToTerminalId(mapTerminal.get(jumper.getToTerminalId()));
			}

			jumperCopy.setStatus(InstallationDrawingAPI.CABLE_STATUS_DEFAULT);
			jumperCopy.setDelFlag(InstallationDrawingAPI.CABLE_FLAG_DEFAULT);

			em.persist(jumperCopy);
		}

		// Create job
		EccmJob newJob = new EccmJob();
		newJob.setJobId(jobid);
		newJob.setSsId(scid);
		newJob.setNewSsId(scCopy.getId());
		em.persist(newJob);
		
		log.info("Completed copy construction!");

		return true;
	}
	
	@Transactional
	public void deleteJobData(String jobid) {

		EccmJob job = em.createNamedQuery("EccmJob.findById", EccmJob.class).setParameter("jobId", jobid).getSingleResult();
		EccmSsConstruction sc = em.find(EccmSsConstruction.class, job.getNewSsId());
		
		if(sc != null) {
			em.createNamedQuery("EccmJumper.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			em.createNamedQuery("EccmCableWork.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			
			// Lock
			em.createNamedQuery("EccmSubstationSet.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			em.createNamedQuery("EccmConstructionSet.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			em.createNamedQuery("EccmLockSet.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			em.createNamedQuery("EccmLockStep.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			
			List<EccmEquipment> equipList = em.createNamedQuery("EccmEquipment.findBySsId", EccmEquipment.class)
					.setParameter("id", sc.getId()).getResultList();
			for (EccmEquipment equip : equipList) {
				List<EccmBase> baseList = em.createNamedQuery("EccmBase.findByEquip", EccmBase.class)
						.setParameter("eqpid", equip.getId()).getResultList();
				
				for (EccmBase base : baseList) {
					em.createNamedQuery("EccmCore.deleteByBase").setParameter("id", base.getId()).executeUpdate();
					em.createNamedQuery("EccmTerminal.deleteByBase").setParameter("id", base.getId()).executeUpdate();
				}
				
				em.createNamedQuery("EccmBase.deleteByEqpId").setParameter("id", equip.getId()).executeUpdate();
				
				// Lock
				em.createNamedQuery("EccmEquipmentLine.deleteByEqp").setParameter("id", equip.getId()).executeUpdate();
				em.createNamedQuery("EccmUnit.deleteByEqp").setParameter("id", equip.getId()).executeUpdate();
			}
			
			em.createNamedQuery("EccmCable.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			em.createNamedQuery("EccmEquipment.deleteBySS").setParameter("id", sc.getId()).executeUpdate();
			
			em.remove(job);
			em.remove(sc);
		}
	}
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/job/delete/{id}")
	@Consumes("application/json")
	@Transactional
	public String deleteJobs(@PathParam("id") String id) {
		try {
			CcmJob oldJob = em.find(CcmJob.class, id);

			if (oldJob != null) {
				if (oldJob.getJobType() == 1) {
					List<CcmJob> listJobChild = em.createNamedQuery("CcmJob.findAllByUperJobId").setParameter("jobId", id).getResultList();
					if (listJobChild != null && listJobChild.size() > 0) {
						for (CcmJob j : listJobChild) {
							j.setJobType(0);
							j.setUperJobId("");
							em.persist(j);
						}
					}
				}
				
				// Remove constructions data
				deleteJobData(id);
				
				em.remove(oldJob);
				String envPath = System.getProperty("jboss.home.dir") + File.separator + "ccm" + File.separator + "JOB_DOCUMENT";
				String folderPath = envPath + File.separator + id;

				File folder = new File(folderPath);
				deleteDir(folder);

				em.createNamedQuery("WorkDrawingData.deleteByJobId").setParameter("id", id).executeUpdate();

				return json.format(new ApiRespone(API_STATUS_OK, "OK"));
			} else {
				return json.format(new ApiRespone(API_STATUS_OK, "entity not found"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiRespone(API_STATUS_NG, e.getMessage()));
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/listDrawing/search")
	public String searchJobForDrawing(CcmJobDTO searchData) {
		log.info("/listDrawing/search");

		JobListData data = new JobListData();

		try {
			Map<String, Object> params = new WeakHashMap<>();
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("select s from CcmJob s where 1=1 and s.ssConstId <> 0 ");

			if (searchData.getMasterSsId() != null && searchData.getMasterSsId() > 0) {
				sqlBuilder.append(" and s.masterSsId =:masterSsId ");
				params.put("masterSsId", searchData.getMasterSsId());
			}

			if (searchData.getType() != null && searchData.getType() > 0) {
				sqlBuilder.append(" and s.jobIndex1 =:type ");
				params.put("type", searchData.getType());
			}

			if (searchData.getGroup() != null && searchData.getGroup() > 0) {
				sqlBuilder.append(" and s.group =:group ");
				params.put("group", searchData.getGroup());
			}

			if (searchData.getStaff() != null && !searchData.getStaff().equals("")) {
				sqlBuilder.append(" and s.name  like :name ");
				params.put("name", "%" + searchData.getStaff() + "%");
			}

			if (searchData.getIsActiveJob() != null && searchData.getIsActiveJob() == 1) {
				sqlBuilder.append(" and s.jobStatus <> 3 ");
			}

			if (searchData.getYear() != null && !searchData.getYear().equals("")) {

				if (searchData.getMonth() != null && !searchData.getMonth().equals("")) {
					String dateStart = searchData.getYear() + searchData.getMonth() + "01";
					String dateEnd = searchData.getYear() + searchData.getMonth() + "31";

					sqlBuilder.append(" and (( s.jobstYmdPlan  <= :dateEnd and s.jobedYmdPlan >= :dateStart ) ");
					sqlBuilder.append(" or ( s.jobstYmdResult  <= :dateEnd and s.jobedYmdResult >= :dateStart ) ) ");

					params.put("dateStart", dateStart);
					params.put("dateEnd", dateEnd);
				} else if ((searchData.getMonth() == null || searchData.getMonth().equals(""))) {
					Integer pYear = Integer.parseInt(searchData.getYear()) - 1;

					String startYear = String.valueOf(pYear) + "04";
					startYear = StringUtils.leftPad(startYear, 6, '0');
					String endYear = searchData.getYear() + "03";
					endYear = StringUtils.leftPad(endYear, 6, '0');
					sqlBuilder.append(" and (( SUBSTRING(s.jobstYmdPlan,1,6)  <= :endYear and SUBSTRING(s.jobedYmdPlan,1,6) >= :startYear ) ");
					sqlBuilder.append(" or ( SUBSTRING(s.jobstYmdResult,1,6)  <= :endYear and SUBSTRING(s.jobedYmdResult,1,6) >= :startYear ) ) ");

					params.put("startYear", startYear);
					params.put("endYear", endYear);
				}
			}

			StringBuilder sqlBuilderEvidence = new StringBuilder();
			sqlBuilderEvidence.append("select f from  WorkDrawingData f  where  f.jobId in (:jobIds) ");

			Query query = em.createQuery(sqlBuilder.toString(), CcmJob.class);

			if (params.size() > 0) {
				for (String key : params.keySet()) {
					query.setParameter(key, params.get(key));
				}
			}
			data.jobs = query.getResultList();

			if (data.jobs != null && data.jobs.size() > 0) {
//				add parent job id if parent not include filter condition
				List<String> jobUperIds = new ArrayList<>();
				List<String> jobParentsId = new ArrayList<>();
				for (CcmJob j : data.jobs) {
					if (j.getJobType() == 1) {
						jobParentsId.add(j.getJobId());
					}
				}

				for (CcmJob j : data.jobs) {
					if (j.getJobType() == 2) {
						if (!jobParentsId.contains(j.getUperJobId()) && j.getUperJobId() != null) {
							jobUperIds.add(j.getUperJobId());
						}
					}
				}

				if (jobUperIds.size() > 0) {
					StringBuilder addSql = new StringBuilder("select s from CcmJob s where 1=1 ");
					addSql.append(" and  s.jobId in (:jobId) ");

					Query query3 = em.createQuery(addSql.toString(), CcmJob.class).setParameter("jobId", jobUperIds);

					data.jobs.addAll(query3.getResultList());
				}

			}

			if (params.size() > 0) {
				for (String key : params.keySet()) {
					query.setParameter(key, params.get(key));
				}
			}

			List<String> listJobId = new ArrayList<>();
			if (data.jobs != null) {
				data.jobs.forEach(j -> {
					listJobId.add(j.getJobId());
				});
			}

			Query query2 = em.createQuery(sqlBuilderEvidence.toString(), WorkDrawingData.class);

			query2.setParameter("jobIds", listJobId.size() == 0 ? "-1" : listJobId);

			data.evidences = query2.getResultList();

			// Constructions
			data.constructions = em.createNamedQuery("CcmSsConstruction.findAll", CcmSsConstruction.class).getResultList();
			

		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiStatus("no data", null));
		}

		JSON parser = new JSON();

		return parser.format(data);
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/columns/get/datas/{id}")
	public String getListParentJob(@PathParam("id") int id) {
		log.info("/api/columns/datas/" + id);
		List<KeyValue> list = new ArrayList<KeyValue>();
		try {
			EccmColMast col = em.find(EccmColMast.class, id);
			List<Object[]> resultList = em.createNativeQuery(col.getFindSql()).getResultList();
			for (Object[] o : resultList) {
				KeyValue d = new KeyValue();
				d.key = String.valueOf(o[0]);
				d.value = (String) o[1];
				list.add(d);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return json.escapeScript(new ApiStatus("no data", e));
		}
		
		return json.escapeScript(list);
	}

	private void deleteDir(File file) {
		File[] jobDatas = file.listFiles();
		if (jobDatas != null) {
			for (File f : jobDatas) {
				File[] documentDatas = f.listFiles();
				if (documentDatas != null) {
					for (File f1 : documentDatas) {
						f1.delete();
					}
				}
				f.delete();
			}
		}
		file.delete();
	}

	// Clone from method searchJob()
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/point/copy-job/search")
	public String searchJobCopy(CcmJobDTO searchData) {
		log.info("/point/copy-job/search");

		JobListData data = new JobListData();
		try {

			Map<String, Object> params = new WeakHashMap<>();
			StringBuilder sqlBuilder = new StringBuilder();
			// Rev1.02 MOD-S
			sqlBuilder.append("select s from CcmJob s, EccmJob e where 1=1 and s.ssConstId <> 0 and s.jobId = e.jobId and e.lockStepStatus = 4 ");
			// Rev1.02 MOD-E

			if (searchData.getMasterSsId() != null && searchData.getMasterSsId() > 0) {
				sqlBuilder.append(" and s.masterSsId =:masterSsId ");
				params.put("masterSsId", searchData.getMasterSsId());
			}

			if (searchData.getType() != null && searchData.getType() > 0) {
				sqlBuilder.append(" and s.jobIndex1 =:type ");
				params.put("type", searchData.getType());
			}

			if (searchData.getGroup() != null && searchData.getGroup() > 0) {
				sqlBuilder.append(" and s.group =:group ");
				params.put("group", searchData.getGroup());
			}

			if (searchData.getStaff() != null && !searchData.getStaff().equals("")) {
				sqlBuilder.append(" and s.name  like :name ");
				params.put("name", "%" + searchData.getStaff() + "%");
			}

			if (searchData.getIsActiveJob() != null && searchData.getIsActiveJob() == 1) {
				sqlBuilder.append(" and s.jobStatus <> 3 ");
			}

			if (searchData.getYear() != null && !searchData.getYear().equals("")) {

				if (searchData.getMonth() != null && !searchData.getMonth().equals("")) {
					String dateStart = searchData.getYear() + searchData.getMonth() + "01";
					String dateEnd = searchData.getYear() + searchData.getMonth() + "31";

					sqlBuilder.append(" and (( s.jobstYmdPlan  <= :dateEnd and s.jobedYmdPlan >= :dateStart ) ");
					sqlBuilder.append(" or ( s.jobstYmdResult  <= :dateEnd and s.jobedYmdResult >= :dateStart ) ) ");

					params.put("dateStart", dateStart);
					params.put("dateEnd", dateEnd);
				} else if ((searchData.getMonth() == null || searchData.getMonth().equals(""))) {
					Integer pYear = Integer.parseInt(searchData.getYear()) - 1;

					String startYear = String.valueOf(pYear) + "04";
					startYear = StringUtils.leftPad(startYear, 6, '0');
					String endYear = searchData.getYear() + "03";
					endYear = StringUtils.leftPad(endYear, 6, '0');
					sqlBuilder.append(" and (( SUBSTRING(s.jobstYmdPlan,1,6)  <= :endYear and SUBSTRING(s.jobedYmdPlan,1,6) >= :startYear ) ");
					sqlBuilder.append(" or ( SUBSTRING(s.jobstYmdResult,1,6)  <= :endYear and SUBSTRING(s.jobedYmdResult,1,6) >= :startYear ) ) ");

					params.put("startYear", startYear);
					params.put("endYear", endYear);
				}
			}

			Query query = em.createQuery(sqlBuilder.toString(), CcmJob.class);
			if (params.size() > 0) {
				for (String key : params.keySet()) {
					query.setParameter(key, params.get(key));
				}
			}

			data.jobs = query.getResultList();
			// Rev1.02 DEL-S
			/*if (data.jobs != null && data.jobs.size() > 0) {
				// add parent job id if parent not include filter condition
				List<String> jobUperIds = new ArrayList<>();
				List<String> jobParentsId = new ArrayList<>();
				for (CcmJob j : data.jobs) {
					if (j.getJobType() == 1) {
						jobParentsId.add(j.getJobId());
					}
				}

				for (CcmJob j : data.jobs) {
					if (j.getJobType() == 2) {
						if (!jobParentsId.contains(j.getUperJobId()) && j.getUperJobId() != null) {
							jobUperIds.add(j.getUperJobId());
						}
					}
				}

				if (jobUperIds.size() > 0) {
					StringBuilder addSql = new StringBuilder("select s from CcmJob s where 1=1 ");
					addSql.append(" and  s.jobId in (:jobId) ");
					query = em.createQuery(addSql.toString(), CcmJob.class).setParameter("jobId", jobUperIds);
					data.jobs.addAll(query.getResultList());
				}

			}*/
			// Rev1.02 DEL-E

			// Add stations and constructions list
			data.stations = em.createNamedQuery("CcmStationMaster.findAll", CcmStationMaster.class).getResultList();
			data.constructions = em.createNamedQuery("CcmSsConstruction.findAll", CcmSsConstruction.class).getResultList();

		} catch (Exception e) {
			e.printStackTrace();
			return json.format(new ApiStatus("no data", null));
		}

		JSON parser = new JSON();
		parser.setDateFormat("yyyy-MM-dd");

		return parser.format(data);
	}

}
