const lstPoweroutFlg = ["無","有"];
const tableID = 1;

// Mapping with database (TABLE_ID=4)
const KEY_NO = {
	JOB_NO: 1, 				//作業番号
	STATION_MASTER: 2, 		//電気所名称
	JOB_INDEX_1: 3, 		//作業種別１
	JOB_NAME: 4, 			//作業件名
	PLAN_START_DATE: 5, 	//開始日
	PLAN_END_DATE: 6, 		//終了日
	ACTUAL_START_DATE: 7, 	//開始日
	ACTUAL_END_DATE: 8, 	//終了日
	POWER_OUT_FLAG: 9, 		//停止
	OPERATION_FLAG: 10, 	//操作
	JOB_DEPARTMENT: 11, 	//作業担当グループ
	CONST_NAME: 50, 		//工事用設備データ
}

const VIEW_COLUMNS = [
	KEY_NO.JOB_NO, KEY_NO.STATION_MASTER, KEY_NO.CONST_NAME, KEY_NO.JOB_INDEX_1, KEY_NO.JOB_NAME, KEY_NO.PLAN_START_DATE, KEY_NO.PLAN_END_DATE, 
	KEY_NO.ACTUAL_START_DATE, KEY_NO.ACTUAL_END_DATE, KEY_NO.POWER_OUT_FLAG, KEY_NO.OPERATION_FLAG, KEY_NO.JOB_DEPARTMENT
]

// Vueオブジェクト
const app = new Vue({
	el: "#my-root",
	data: {
		ssName: "",
		selectedSsId: -1,
		selectedJobId: -1,
		items: [],
		filteredList: [],
		columnMasts: [],
		viewColMasts: [],
		findColMasts: [],
		listIndex1: [],
		listDepartment: [],
		listStation: [],
		listConstruction: [],
		colKeyNo: KEY_NO,
		viewColIndexs: {},
		isLoading: false,
		isLoaded: false
	},

	created: function() {
		var urlparams = decodeURI(location.search.substring(1));
        var sss = urlparams.split("&");
        var ss = sss[0].split("=");
        sss.forEach(v => {
            let args = v.split("=");
            switch (args[0]) {
                case "jobid":
                    this.selectedJobId = args[1];
					break;
				case "ssid":
					this.selectedSsId = parseInt(args[1]);
					break;
            }
        });
	},

	mounted: function() {
		this.getColumns();
	},

	methods: {
		jump2Top: function() {
			location.href = PROJECT_NAME + "?ssid="+this.selectedSsId;
		},

		jump2Back: function(id) {
			location.href = PROJECT_NAME + "?ssid="+this.selectedSsId;
		},
		
		getColumns: function() {

			this.isLoading = true;
			this.isLoaded = false;
			axios.get(PROJECT_NAME +"/api/columns/"+tableID)
			.then((response) => {
				// Original column list
				this.columnMasts = response.data;
				
				var listIndex1Col = this.columnMasts.find(c => c.col.keyNo == this.colKeyNo.JOB_INDEX_1);
				listIndex1Col.datas.forEach(i => { 
					this.listIndex1[i.key] = i;
				});

				var stationMasterCol = this.columnMasts.find(c => c.col.keyNo == this.colKeyNo.STATION_MASTER);
				stationMasterCol.datas.forEach(i => { 
					this.listStation[i.key] = i;
				});

				var departmentCol = this.columnMasts.find(c => c.col.keyNo == this.colKeyNo.JOB_DEPARTMENT);
				departmentCol.datas.forEach(i => { 
					this.listDepartment[i.key] = i;
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

					switch(this.viewColMasts[i].col.keyNo) {
						case KEY_NO.PLAN_START_DATE:
							this.viewColIndexs.PLAN_START_DATE = KEY_NO.PLAN_START_DATE;
							break;
						case KEY_NO.PLAN_END_DATE:
							this.viewColIndexs.PLAN_END_DATE = KEY_NO.PLAN_END_DATE;
							break;
						case KEY_NO.ACTUAL_START_DATE:
								this.viewColIndexs.ACTUAL_START_DATE = KEY_NO.ACTUAL_START_DATE;
								break;
						case KEY_NO.ACTUAL_END_DATE:
							this.viewColIndexs.ACTUAL_END_DATE = KEY_NO.ACTUAL_END_DATE;
							break;
					}
				}
				
				this.getItemList();
			}).catch((error) => {
				app.$alert("error[" + error + "]");
			});
		},
				
		getItemList: function() {

			this.isLoading = true;
			axios.get(PROJECT_NAME +"/api/point/job/get/" + this.selectedSsId)
			.then((response) => {
				this.data = response.data;
				
				this.data.stations.forEach(v=>{
					this.listStation[v.id] = v;
				})

				this.data.constructions.forEach(v => { this.listConstruction[v.id] = v; })
				
				let parentJobId = null;
				this.data.jobs.forEach(v=>{
					let item = {"id":0,"names":[]};
					item.id = v.jobId;
					item.status = v.jobStatus;
					item.jobstYmdPlan = v.jobstYmdPlan;
					item.names[this.colKeyNo.JOB_NO] = v.jobId;
					item.names[this.colKeyNo.STATION_MASTER] = v.masterSsId == null?"" : this.listStation[v.masterSsId].name;
					item.names[this.colKeyNo.CONST_NAME] = v.ssConstId == null?"" : this.listConstruction[v.ssConstId].constDetail;
					item.names[this.colKeyNo.JOB_INDEX_1] = v.jobIndex1 == null ?"":this.listIndex1[v.jobIndex1].value ;
					item.names[this.colKeyNo.JOB_NAME] = v.jobName;
					item.names[this.colKeyNo.PLAN_START_DATE] = v.jobstYmdPlan == null ?"" : v.jobstYmdPlan.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
					item.names[this.colKeyNo.PLAN_END_DATE] = v.jobedYmdPlan == null ?"" : v.jobedYmdPlan.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
					item.names[this.colKeyNo.ACTUAL_START_DATE] = v.jobstYmdResult == null ?"" : v.jobstYmdResult.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
					item.names[this.colKeyNo.ACTUAL_END_DATE] = v.jobedYmdResult == null ?"" : v.jobedYmdResult.replace(/(\d{4})(\d{2})(\d{2})/g, '$1/$2/$3');
					item.names[this.colKeyNo.POWER_OUT_FLAG] =  v.poweroutFlg == null ?"":lstPoweroutFlg[v.poweroutFlg];
					item.names[this.colKeyNo.OPERATION_FLAG] =  v.operationFlg == null ?"":lstPoweroutFlg[v.operationFlg];
					item.names[this.colKeyNo.JOB_DEPARTMENT] = v.group == null?"":this.listDepartment[v.group].value;
					item.opened = false;
					item.jobType = v.jobType;
					item.uperJobId = v.uperJobId
					this.items.push(item);
					if(this.selectedJobId != null &&  v.jobId == this.selectedJobId && v.jobType == 2){
						parentJobId = v.uperJobId;
					}
				})
				
				this.items.sort(function(a,b){ return b.jobstYmdPlan- a.jobstYmdPlan });
				this.filteredList = this.items.filter(v =>{return v.jobType == 0 || v.jobType == 1});
				
				if(parentJobId != null){
					for(i = 0 ; i < this.filteredList.length; i++){
						let row = this.filteredList[i];
						if(this.filteredList[i].id == parentJobId){
							this.onExploreItem(i,row);
						}
					}
				}
			})
			.catch((error) => {
				app.$alert("error[" + error + "]");
			}).finally(() => { 
				this.isLoading = false ;
				this.isLoaded = true;
			});
		},
		
		menuAction : function(menuId, flg) {

			if(this.selectedJobId == -1) {
				app.$alert("作業件名を選択してください。");
				return;
			}

			if(menuId == "undefined"){
				return;
			}
			let link = MENU_LINK[menuId];
			let url = link.url;
			if (link == null) {
				this.$alert("準備中です。", "情報");
				return;
			}
			if(this.selectedJobId == null ||this.selectedJobId === -1 ){
				
			} else {
				if(link.type == 1){
					if(link.url.endsWith(".html") ){
						url += "?jobid="+this.selectedJobId
					} else{
						url += "&jobid="+this.selectedJobId
					}
				}
			}
			
			location.href = url;
		},

		rowClassName : function({row, column, rowIndex, columnIndex}){
			if(row.status == 2){
				return 'warning-row';
			}
			return '';
		},

		onSort : function(a,b){
			return a.jobstYmdPlan - b.jobstYmdPlan;
		},

		onExploreItem: function(idx, row){
			row.opened = true;
			let uperJobId = row.id;
			let data = this.items.filter(v =>{return v.uperJobId == uperJobId});
			if(data != null && data.length > 0){
				
				data.sort(function(a, b){return a.jobstYmdPlan - b.jobstYmdPlan })
				data.forEach(d =>{
					this.filteredList.splice(idx + 1,0,d);
				})
			}
		},

		onCloseItem: function(idx,row){
			row.opened = false;
			let uperJobId = row.id;
			this.filteredList = this.filteredList.filter(v =>{ return v.uperJobId != uperJobId});
		}
		
	},
	computed:{
		
	},
	
});