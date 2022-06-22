/**
// Control Cables Management Tool(制御ケーブル管理ツール)
//
// (C)2021 Toshiba Energy Systems & Solutions Corporation
//    All Rights Reserved.
//
// パッケージ名：
// ファイル名　：ステップ一覧(job.js)
//
//
// [変更履歴]
//
// (初版作成)
//   設計         2022/02/18   	承認：      調査：     担当：（TSDV）TuLV
//   製造             /  /     	承認：      調査：     担当：
//   単体試験         /  /     	承認：      調査：     担当：
//
// Rev.             /  /     	承認：      調査：     担当：
//   変更理由
//
//===============================================================================
//  Version     変更日            変更者              変更内容
//-------------------------------------------------------------------------------------------------------------------------------------------
//  1.01       2022/02/18    (TSDV)TheNV       	- Reuse from job.js
//-------------------------------------------------------------------------------------------------------------------------------------------
*/
const lstPoweroutFlg = ["無","有"];
const tableID = 2;
const numberRegex = /[0-9]/;
const lstFlg = [{key: "0", value: "無"},{key: "1", value: "有"}]
const lstJobStatus = [{key: "0", value: "計画中"},{key: "1", value: "実施中"},{key: "2", value: "実施済"},{key: "3", value: "中止"}]

// Mapping with database (TABLE_ID=2)
const KEY_NO = {
	JOB_NO: 1, 				//作業番号
	CONST_NO: 2, 			//工事件名番号
	STATION_MASTER: 3, 		//電気所名称
	JOB_TYPE_DISP: 4, 		//工事種別
	JOB_NAME: 5, 			//作業件名
	OPERATION_DATE: 6, 		//運開年月日
	JOB_INDEX_1: 7, 		//作業種別１
	JOB_INDEX_2: 8, 		//作業種別２
	PLAN_START_DATE: 9, 	//開始日
	PLAN_END_DATE: 10, 		//終了日
	ACTUAL_START_DATE: 11, 	//開始日
	ACTUAL_END_DATE: 12, 	//終了日
	POWER_OUT_FLAG: 13, 	//停止
	OPERATION_FLAG: 14, 	//操作
	CONST_PLACE: 15, 		//工事担当箇所
	CONST_STAFF: 16, 		//工事担当者
	JOB_STATUS: 17, 		//作業実施状況
	JOB_DEPARTMENT: 18, 	//作業担当グループ
	NAME: 19, 				//担当者
	COMP: 20, 				//取引先
	JOB_TYPE_INS: 21, 		//件名タイプ
	UPPER_JOB_ID: 22, 		//親作業番号
	CONST_NAME: 50, 		//工事用設備データ
}

// Rev1.01 MOD-S
const VIEW_COLUMNS = [
	KEY_NO.JOB_NO, KEY_NO.STATION_MASTER, KEY_NO.JOB_TYPE_DISP, KEY_NO.JOB_NAME, KEY_NO.PLAN_START_DATE, KEY_NO.PLAN_END_DATE,
	KEY_NO.ACTUAL_START_DATE, KEY_NO.ACTUAL_END_DATE, KEY_NO.POWER_OUT_FLAG, KEY_NO.OPERATION_FLAG, KEY_NO.JOB_DEPARTMENT, KEY_NO.CONST_PLACE, KEY_NO.CONST_STAFF
]
// Rev1.01 MOD-E

const DIALOG_COLUMNS = [
	KEY_NO.CONST_NO, KEY_NO.JOB_TYPE_INS, KEY_NO.UPPER_JOB_ID, KEY_NO.STATION_MASTER, KEY_NO.CONST_NAME, KEY_NO.JOB_NAME, KEY_NO.OPERATION_DATE, KEY_NO.JOB_INDEX_1, KEY_NO.JOB_INDEX_2, 
	KEY_NO.PLAN_START_DATE, KEY_NO.PLAN_END_DATE, KEY_NO.ACTUAL_START_DATE, KEY_NO.ACTUAL_END_DATE, KEY_NO.POWER_OUT_FLAG, KEY_NO.OPERATION_FLAG, 
	KEY_NO.JOB_STATUS, KEY_NO.JOB_DEPARTMENT, KEY_NO.NAME, KEY_NO.COMP
]

// Vueオブジェクト
const app = new Vue({
	el: "#my-root",
	data: {
		items: [],
		tempJobId: -1,
		columnMasts: [],
		viewColMasts: [],
		listIndex1: [],
		listDepartment:[],
		listStation: [],
		listConstruction :[],
		validJobId:[],
		ssList: [],
		selectedSsId: '',
		selectedScId: '',
		selectedSs: {},
		constructionYear:'',
		constructionMonth:'',
		lstIndex1:[],
		lstDepartment:[],
		filteredList:[],
		constructionType:'',
		group:'',
		constructionStaff:'',
		insertColMast:[],
		createDialog: {
			mode: "create",	
			visible: false,	
			title: "工事件名作成ダイアログ",
			disabledSave: false,
			data:[]
		},
		deleteDialog:{
			visible: false,
			id:0,
		},
		colKeyNo: KEY_NO,
		viewColIndexs: {},
		insertColIndexs: {},
		isLoading: false,
	},

	created: function() {
    // Rev1.01 ADD-S
		const urlSearchParams = new URLSearchParams(window.location.search);
		const params = Object.fromEntries(urlSearchParams.entries());
		this.srcJobId = params['jobid'];
		this.ssId = params['ssid'];
		this.selectedSsId = this.ssId;
		this.getJobDetail();
    // Rev1.01 ADD-E
		this.getToday();
	},

	mounted: function() {
		this.getColumns();
	},

	methods: {
		// トップ画面へ
		jump2Top: function() {
			location.href = PROJECT_NAME + "/pages/listSubstation.html";
		},

		// 前画面へ
		jump2Back: function(id) {
			history.back();
		},
		
		getColumns: function() {
			this.isLoading = true;

			axios.get(PROJECT_NAME +"/api/columns/"+tableID)
			.then((response) => {
				// Original column list
				this.columnMasts = response.data;
				
				var listIndex1Col = this.columnMasts.find(c => c.col.keyNo == this.colKeyNo.JOB_INDEX_1);
				listIndex1Col.datas.forEach(i => { 
					this.listIndex1[i.key] = i;
					this.lstIndex1.push(i);
				});

				var stationMasterCol = this.columnMasts.find(c => c.col.keyNo == this.colKeyNo.STATION_MASTER);
				stationMasterCol.datas.forEach(i => { 
					this.listStation[i.key] = i;
					this.ssList.push(i);
				});

				var departmentCol = this.columnMasts.find(c => c.col.keyNo == this.colKeyNo.CONST_PLACE);
				departmentCol.datas.forEach(i => { 
					this.listDepartment[i.key] = i;
					this.lstDepartment.push(i);
				});

				var tWidth = 0;
				VIEW_COLUMNS.forEach(keyNo => {
					var col = this.columnMasts.find(c => c.col.keyNo == keyNo && c.col.listViewFlg == 1);
					if(col != undefined) {
						tWidth += col.width;
						this.viewColMasts.push(col);
					}
				});
				this.tableWidth = "width:" + (535 + tWidth) + "px";
				
				// Get index of some nesscessary columns
				for (let i = 0; i < this.viewColMasts.length; i++) {
					if(this.viewColMasts[i].col.keyNo == this.colKeyNo.PLAN_START_DATE) {
						this.viewColIndexs.PLAN_START_DATE = i;
					}
					if(this.viewColMasts[i].col.keyNo == this.colKeyNo.PLAN_END_DATE) {
						this.viewColIndexs.PLAN_END_DATE = i;
					}
					if(this.viewColMasts[i].col.keyNo == this.colKeyNo.ACTUAL_START_DATE) {
						this.viewColIndexs.ACTUAL_START_DATE = i;
					}
					if(this.viewColMasts[i].col.keyNo == this.colKeyNo.ACTUAL_END_DATE) {
						this.viewColIndexs.ACTUAL_END_DATE = i;
					}
				}

				// Dialog columns
				DIALOG_COLUMNS.forEach(keyNo => {
					var col = this.columnMasts.find(c => c.col.keyNo == keyNo)
					if(col != undefined) {
						if(keyNo == this.colKeyNo.POWER_OUT_FLAG || keyNo == this.colKeyNo.OPERATION_FLAG) {
							col.datas = lstFlg
						}
						if(keyNo == this.colKeyNo.JOB_STATUS) {
							col.datas = lstJobStatus
						}
						this.insertColMast.push(col);
					}
				});

				// Get index of some nesscessary columns
				for (let i = 0; i < this.insertColMast.length; i++) {
					if(this.insertColMast[i].col.keyNo == this.colKeyNo.JOB_TYPE_INS) {
						this.insertColIndexs.JOB_TYPE_INS = i;
					}
					if(this.insertColMast[i].col.keyNo == this.colKeyNo.UPPER_JOB_ID) {
						this.insertColIndexs.UPPER_JOB_ID = i;
					}					
				}

				this.searchJob();
			})
		},
		
		searchJob: function () {

			this.isLoading = true;
			if(!this.validSearchDate()){
				this.isLoading = false;
				return
			}
			let temp = false;
			if(this.constructionMonth < 4){
				temp = true
			}
			if(this.constructionMonth != '' &&  parseInt(this.constructionMonth) < 10){
				this.constructionMonth = "0"+ parseInt(this.constructionMonth)
			}
			
			let searchYear = +this.constructionYear + (temp ? +1:+0) +"";
			let data = {
				masterSsId: this.selectedSsId ===""? null: parseInt(this.selectedSsId),
				year: this.constructionYear ===""? this.constructionYear:searchYear,
				month: this.constructionMonth,
				type: this.constructionType === ""?null: parseInt(this.constructionType),
				group: this.group === ""? null:parseInt(this.group),
				staff: this.constructionStaff
			}

			axios.post(PROJECT_NAME +"/api/point/copy-job/search", data)
			.then((response) => {
				// Get stations and constructions data
				response.data.constructions.forEach(v => { this.listConstruction[v.id] = v; })

				// Mapping jobs data
				this.mappingData(response.data.jobs);
				// Rev1.01 MOD-S
				this.filteredList = this.items;
				// Rev1.01 MOD-E
			})
			.catch((error) => {
				app.$alert("error[" + error + "]");
			})
			.finally(() => {
				this.isLoading = false;
			});
		},
		
		mappingData: function (jobList){
			this.items = [];
			jobList.forEach(v => {
				let item = { "id":0, "names":[] };
				item.id = v.jobId;
				item.masterSsId = v.masterSsId;
				item.sJobstYmdPlan = v.jobstYmdPlan == null ?"":v.jobstYmdPlan.substring(0,6) ;
				item.sJobedYmdPlan =  v.jobedYmdPlan == null ?"":v.jobedYmdPlan.substring(0,6);
				item.sJobstYmdResult = v.jobstYmdResult == null ?"":v.jobstYmdResult.substring(0,6) ;
				item.sJobedYmdResult =  v.jobedYmdResult == null ?"":v.jobedYmdResult.substring(0,6);
				item.cType = v.jobIndex1;
				item.cGroup = v.group;
				item.cStaff = v.name;
				item.status = v.jobStatus;
				item.data = v;
				item.names[this.colKeyNo.JOB_NO] = v.jobId;
				item.names[this.colKeyNo.STATION_MASTER] = v.masterSsId == null?"":this.listStation[v.masterSsId].value;
				// Rev1.01 DEL-S
//				item.names[this.colKeyNo.CONST_NAME] = v.ssConstId == null ? "" : this.listConstruction[v.ssConstId].constDetail;
				// Rev1.01 DEL-E
				item.names[this.colKeyNo.JOB_TYPE_DISP] = v.jobIndex1 == null ?"":this.listIndex1[v.jobIndex1].value ;
				item.names[this.colKeyNo.JOB_NAME] = v.jobName;
				item.names[this.colKeyNo.PLAN_START_DATE] = v.jobstYmdPlan == null ?"" : v.jobstYmdPlan.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
				item.names[this.colKeyNo.PLAN_END_DATE] = v.jobedYmdPlan == null ?"" : v.jobedYmdPlan.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
				item.names[this.colKeyNo.ACTUAL_START_DATE] = v.jobstYmdResult == null ?"" : v.jobstYmdResult.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
				item.names[this.colKeyNo.ACTUAL_END_DATE] = v.jobedYmdResult == null ?"" : v.jobedYmdResult.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
				item.names[this.colKeyNo.POWER_OUT_FLAG] =  v.poweroutFlg == null ?"":lstPoweroutFlg[v.poweroutFlg];
				item.names[this.colKeyNo.OPERATION_FLAG] =  v.operationFlg == null ?"":lstPoweroutFlg[v.operationFlg];
				item.names[this.colKeyNo.JOB_DEPARTMENT] = v.group == null?"":this.listDepartment[v.group].value;
				// Rev1.01 DEL-S
//				item.names[this.colKeyNo.NAME] = v.name;
				// Rev1.01 DEL-E
				// Rev1.01 ADD-S
				item.names[this.colKeyNo.CONST_PLACE] = v.group == null?"":this.listDepartment[v.group].value;
				item.names[this.colKeyNo.CONST_STAFF] = v.name;
				// Rev1.01 ADD-E
				item.opened = false;
				this.items.push(item);
			})

			this.items.sort(function(a, b){
				return b.data.jobstYmdPlan - a.data.jobstYmdPlan
			});
		},

		validSearchDate: function(){
			if(this.constructionYear == '' && this.constructionMonth == ''){
				return true;
			}
			if(this.constructionMonth == '' && numberRegex.test(this.constructionYear) && this.constructionYear.toString().length <= 4){
				return true;
			}
			if(this.constructionYear == '' && numberRegex.test(this.constructionMonth)){
				app.$alert("年を入力してください。");
				return false;
			}
			if(!numberRegex.test(this.constructionYear) || !numberRegex.test(this.constructionMonth)  ){
				app.$alert("正しい月と年を入力してください");
				return false;
			}
			
			if(this.constructionMonth > 12 || this.constructionMonth < 1){
				app.$alert("正しい月と年を入力してください");
				return false;
			}
			if(this.constructionYear.length > 4){
				app.$alert("正しい月と年を入力してください");
				return false;
			}

			return true;			
		},

		getToday: function(){
			var today = new Date();
			var dd = String(today.getDate()).padStart(2, '0');
			var mm = String(today.getMonth() + 1).padStart(2, '0'); // January
																	// is 0!
			var yyyy = today.getFullYear();

			if(mm < 4){
				yyyy = yyyy - 1;
			}

			this.constructionYear = yyyy;
			// Rev1.01 MOD-S
			this.constructionMonth = '';
			// Rev1.01 MOD-E
			return yyyy +'年 ' +mm +'月 '+ dd+'日';
		},

		rowClassName : function({row, column, rowIndex, columnIndex}){
			if(row.status == 2){
				return 'warning-row';
			}
			return '';
		},
		// Rev1.01 DEL-S
		/*
		openDialog:function(mode,item) {

			if(mode == 'delete'){
				this.deleteDialog.visible = true;
				this.deleteDialog.id = item.data.jobId;
			} else {
				this.getParentJobId();
				this.createDialog.visible = true;
				this.createDialog.mode = mode;
			}

			if(mode == 'update' || mode == 'view'){
				if(mode == 'update'){
					this.createDialog.title = '工事件名修正ダイアログ';
				} else {
					this.createDialog.title = '工事件名表示ダイアログ'
				}

				this.createDialog.data[this.colKeyNo.CONST_NO] = item.data.jobId;
				this.createDialog.data[this.colKeyNo.JOB_TYPE_INS] = item.data.jobType;
				this.createDialog.data[this.colKeyNo.UPPER_JOB_ID] = item.data.uperJobId;
				this.createDialog.data[this.colKeyNo.STATION_MASTER] = item.data.masterSsId;
				this.createDialog.data[this.colKeyNo.CONST_NAME] = item.data.ssConstId;
				this.createDialog.data[this.colKeyNo.JOB_NAME] = item.data.jobName;
				this.createDialog.data[this.colKeyNo.OPERATION_DATE] = item.data.operationYmd;
				this.createDialog.data[this.colKeyNo.JOB_INDEX_1] = item.data.jobIndex1;
				this.createDialog.data[this.colKeyNo.JOB_INDEX_2] = item.data.jobIndex2;
				this.createDialog.data[this.colKeyNo.PLAN_START_DATE] = item.data.jobstYmdPlan;
				this.createDialog.data[this.colKeyNo.PLAN_END_DATE] = item.data.jobedYmdPlan;
				this.createDialog.data[this.colKeyNo.ACTUAL_START_DATE] = item.data.jobstYmdResult;
				this.createDialog.data[this.colKeyNo.ACTUAL_END_DATE] = item.data.jobedYmdResult;
				this.createDialog.data[this.colKeyNo.POWER_OUT_FLAG] = item.data.poweroutFlg;
				this.createDialog.data[this.colKeyNo.OPERATION_FLAG] = item.data.operationFlg;
				this.createDialog.data[this.colKeyNo.JOB_STATUS] = item.data.jobStatus;
				this.createDialog.data[this.colKeyNo.JOB_DEPARTMENT] = item.data.group;
				this.createDialog.data[this.colKeyNo.NAME] = item.data.name;
				this.createDialog.data[this.colKeyNo.COMP] = item.data.comp;

				if(item.data.jobIndex1 != null &&  item.data.jobIndex1 != ''){
					this.findUpdateCol(this.insertColMast, app.$refs.fdItemSelect1, this.colKeyNo.JOB_INDEX_1, item.data.jobIndex1)
				}

				if(item.data.masterSsId != null &&  item.data.masterSsId != ''){
					this.findUpdateCol(this.insertColMast, app.$refs.fdItemSelect1, this.colKeyNo.STATION_MASTER, item.data.masterSsId)
				}

				this.createDialog.disabledSave = this.checkSaveButton();
			}

			if(mode == 'insert'){
				this.createDialog.title = '工事件名作成ダイアログ'
				axios.get(PROJECT_NAME +"/api/job/generateJobNo").then((response) => {
					this.updateFormItem(app.$refs.fdItemSelect1, this.colKeyNo.CONST_NO, response.data.data);
					this.createDialog.data[this.colKeyNo.CONST_NO] = response.data.data;
				})
				.catch((error) => {
					app.$alert("error[" + error + "]");
				});
				this.createDialog.data[this.colKeyNo.JOB_TYPE_INS] = "";
				this.createDialog.data[this.colKeyNo.UPPER_JOB_ID] = "";
				this.createDialog.data[this.colKeyNo.STATION_MASTER] = this.selectedSsId != null ? this.selectedSsId :"";
				this.createDialog.data[this.colKeyNo.CONST_NAME] = "";
				this.createDialog.data[this.colKeyNo.JOB_NAME] = "";
				this.createDialog.data[this.colKeyNo.OPERATION_DATE] = "";
				this.createDialog.data[this.colKeyNo.JOB_INDEX_1] = this.constructionType != null ? this.constructionType :"";
				this.createDialog.data[this.colKeyNo.JOB_INDEX_2] = "";
				this.createDialog.data[this.colKeyNo.PLAN_START_DATE] = "";
				this.createDialog.data[this.colKeyNo.PLAN_END_DATE] = "";
				this.createDialog.data[this.colKeyNo.ACTUAL_START_DATE] = "";
				this.createDialog.data[this.colKeyNo.ACTUAL_END_DATE] = "";
				this.createDialog.data[this.colKeyNo.POWER_OUT_FLAG] = 1;
				this.createDialog.data[this.colKeyNo.OPERATION_FLAG] = 1;
				this.createDialog.data[this.colKeyNo.JOB_STATUS] = 0;
				this.createDialog.data[this.colKeyNo.JOB_DEPARTMENT] = this.group != null ? this.group :"";
				this.createDialog.data[this.colKeyNo.NAME] = getUsernameFromCookie();
				this.createDialog.data[this.colKeyNo.COMP] = "";

				if(this.constructionType != null){
					this.findUpdateCol(this.insertColMast, app.$refs.fdItemSelect1, this.colKeyNo.JOB_INDEX_1, this.constructionType)
				}

				if(this.selectedSsId != null && this.selectedSsId != ''){
					this.findUpdateCol(this.insertColMast, app.$refs.fdItemSelect1, this.colKeyNo.STATION_MASTER, this.selectedSsId)
				}

				this.createDialog.disabledSave = this.checkSaveButton();
			}

			return false;
		},
		hideDialog: function (dialog) {
			this.createDialog.data = [];
			this.createDialog.visible = false;
			this.deleteDialog.visible = false;
		},

		onChangeValue: function(event, keyNo, selected) {
			if(selected != null){
				this.createDialog.data[keyNo] = "" + selected;
			} else {
				this.createDialog.data[keyNo] = "";
			}

			this.createDialog.disabledSave = this.checkSaveButton();

			// Update the selection of job index 2 based on job index 1 || construction list based on station master
			if(keyNo == this.colKeyNo.STATION_MASTER || keyNo == this.colKeyNo.JOB_INDEX_1){
				this.findUpdateCol(this.insertColMast, app.$refs.fdItemSelect1, keyNo, selected);
			}
		},

		updateFormItem: function(ref, keyNo, value){
			if (ref != null) {
				let col = ref.find(x => x.keyno === keyNo);
				col.selected = value == null ? "":value;
				col._events.action[0].fns(null, col.keyno, "");
			}
		},

		findUpdateCol: function(list, ref, keyNo, selected) {
			console.log("findUpdateCol start keyNo:" + keyNo + ", selected:" + selected);

			let item = list.find(v => v.col.keyNo == keyNo);
			if (item.col.inputType != 1) return;
			let id = item.col.keyNo;
			list.some(v => {
				if (v.col.findChainFlg === 1 && v.col.findChainColId === id) {
					v.datas = [];
					if (selected !== "") {
						axios.get(PROJECT_NAME + "/api/columns/datas/" + v.col.id + "/" + selected)
							.then((response) => {
								console.log("findUpdateCol finish keyNo:" + keyNo + ", selected:" + selected);
								v.datas = response.data;
							})
							.catch((error) => {
								app.$alert("error[" + error + "]");
							});
					}
					if (ref != null) {
						let col = ref.find(x => x.mast.col.id === v.col.id);
						col.selected = selected;
						// Revoke the onChangeValue()
						col._events.action[0].fns(null, col.keyno, selected);
					}
					return true;
				}
			});
		},

		getParentJobId: function(){
			var upperCol = this.insertColMast.find(v => v.col.keyNo == this.colKeyNo.UPPER_JOB_ID);
			axios.get(PROJECT_NAME + "/api/columns/get/datas/" + upperCol.col.id)
			.then((response) => {
				upperCol.datas = response.data;
			}).catch((error) => {
				app.$alert("error[" + error + "]");
			});
		},

		checkSaveButton: function(){
			if(this.createDialog.mode == 'insert' || this.createDialog.mode == 'update'){
				var disabled = false;
				const requiredKeyNo = this.insertColMast.filter(v => v.col.requiredFlg == 1).map(v => v.col.keyNo);
				for (let i = 0; i < requiredKeyNo.length; i++) {
					const key = requiredKeyNo[i];

					// Skip to check upper job id if the job type is not child
					if(key == this.colKeyNo.UPPER_JOB_ID && this.createDialog.data[this.colKeyNo.JOB_TYPE_INS] != 2) {
						continue;
					}

					if(this.createDialog.data[key] === '') {
						disabled = true;
						break;
					}
				}
			}

			return disabled;
		},

		deleteJob: function(){
			this.isLoading = true;
			axios.get(PROJECT_NAME + "/api/job/delete/" + this.deleteDialog.id )
			.then((response) => {
				if(response.data.status == "ok") {
					this.hideDialog();
					this.searchJob();
				} else {
					app.$alert("削除失敗しました。");
				}
			})
			.catch((error) => {
				app.$alert("error[" + error + "]");
			})
			.finally(() => { this.isLoading = false; });
		},

		saveJob: function(){
			this.isLoading = true;
			if(this.createDialog.mode =='view'){
				this.hideDialog();
				this.isLoading = false;
				return;
			}

			if(this.createDialog.data[this.colKeyNo.PLAN_START_DATE] !== "" && this.createDialog.data[this.colKeyNo.PLAN_END_DATE] !==""
				&& this.createDialog.data[this.colKeyNo.PLAN_START_DATE] > this.createDialog.data[this.colKeyNo.PLAN_END_DATE] ){
				app.$alert("開始日は終了日よりも小さくする必要があります");
				this.isLoading = false;
				return;
			}

			if(this.createDialog.data[this.colKeyNo.ACTUAL_START_DATE] !== "" && this.createDialog.data[this.colKeyNo.ACTUAL_END_DATE] !==""
				&& this.createDialog.data[this.colKeyNo.ACTUAL_START_DATE] > this.createDialog.data[this.colKeyNo.ACTUAL_END_DATE]){
				app.$alert("開始日は終了日よりも小さくする必要があります");
				this.isLoading = false;
				return;
			}

			let data = {
				jobId : this.createDialog.data[this.colKeyNo.CONST_NO],
				jobType: this.createDialog.data[this.colKeyNo.JOB_TYPE_INS] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.JOB_TYPE_INS]),
				uperJobId: this.createDialog.data[this.colKeyNo.UPPER_JOB_ID],
				masterSsId: this.createDialog.data[this.colKeyNo.STATION_MASTER] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.STATION_MASTER]),
				ssConstId: this.createDialog.data[this.colKeyNo.CONST_NAME] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.CONST_NAME]),
				jobName: this.createDialog.data[this.colKeyNo.JOB_NAME],
				operationYmd: this.createDialog.data[this.colKeyNo.OPERATION_DATE],
				jobIndex1: this.createDialog.data[this.colKeyNo.JOB_INDEX_1] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.JOB_INDEX_1]),
				jobIndex2:  this.createDialog.data[this.colKeyNo.JOB_INDEX_2] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.JOB_INDEX_2]),
				jobstYmdPlan: this.createDialog.data[this.colKeyNo.PLAN_START_DATE] ,
				jobedYmdPlan: this.createDialog.data[this.colKeyNo.PLAN_END_DATE] ,
				jobstYmdResult: this.createDialog.data[this.colKeyNo.ACTUAL_START_DATE] ,
				jobedYmdResult: this.createDialog.data[this.colKeyNo.ACTUAL_END_DATE] ,
				poweroutFlg: this.createDialog.data[this.colKeyNo.POWER_OUT_FLAG] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.POWER_OUT_FLAG]),
				operationFlg:  this.createDialog.data[this.colKeyNo.OPERATION_FLAG] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.OPERATION_FLAG]),
				jobStatus: this.createDialog.data[this.colKeyNo.JOB_STATUS] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.JOB_STATUS]),
				group: this.createDialog.data[this.colKeyNo.JOB_DEPARTMENT] === ""? null: parseInt(this.createDialog.data[this.colKeyNo.JOB_DEPARTMENT]),
				name: this.createDialog.data[this.colKeyNo.NAME],
				comp: this.createDialog.data[this.colKeyNo.COMP],
			}

			axios.post(PROJECT_NAME + "/api/job/edit", data)
			.then((response) => {
				var data = response.data;
				if(data.status == "ok") {
					this.hideDialog();
					this.searchJob();
				} else {
					app.$alert("error[" + data.data + "]");
				}
			})
			.catch((error) => {
				app.$alert("error[" + error + "]");
			})
			.finally(() => {
				this.isLoading = false;
			});
		},

		onSort: function(a,b){
			return a.data.jobstYmdPlan - b.data.jobstYmdPlan;
		},

		onChangeJobType: function(){
			this.createDialog.data[this.colKeyNo.UPPER_JOB_ID] = ""
			app.$refs.uperJobIdSelection[0].value = ""
			this.createDialog.disabledSave = this.checkSaveButton();
		},

		changeUperJobId: function(){
			this.createDialog.disabledSave = this.checkSaveButton();
		},

		onExploreItem: function(idx, row){
			row.opened = true;
			let uperJobId = row.data.jobId;
			let data = this.items.filter(v =>{ return v.data.uperJobId == uperJobId });
			if(data != null && data.length > 0){
				data.sort(function(a, b){ return a.data.jobstYmdPlan - b.data.jobstYmdPlan; })
				data.forEach(d =>{ this.filteredList.splice(idx + 1, 0, d); })
			}
		},

		onCloseItem: function(idx, row){
			row.opened = false;
			let uperJobId = row.data.jobId;
			this.filteredList = this.filteredList.filter(v =>{ return v.data.uperJobId != uperJobId; });
		},
		*/
		// Rev1.01 DEL-E

		// Rev1.01 ADD-S
		openCopyStepScreen: function(row) {
			let url = PROJECT_NAME + "/pages/lockStepListCopy.html?"
			url += "&jobid=" + row.id + "&ssid=" + this.ssId + "&srcJobId=" + this.srcJobId;
			location.href = url;
		},

		getJobDetail: function(){
			axios.get(PROJECT_NAME + "/api/lockStep/findJobDetail/" + this.srcJobId)
			.then((response) => {
				let content = response.data;
				if(content.status == "ok") {
					this.group = content.data.group;
				} else {
					app.$alert(content.data, "エラー情報");
				}
			})
			.catch((error) => {
				app.$alert(error , "エラー情報");
			});

		},
		// Rev1.01 ADD-E
	},
	computed:{
	},


});
