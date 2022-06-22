/**
// Control Cables Management Tool(制御ケーブル管理ツール)
//
// (C)2019 Toshiba Energy Systems & Solutions Corporation
//    All Rights Reserved.
//
// パッケージ名：制御ケーブル管理パッケージ
// ファイル名　：(JobListData)
//
//
// [変更履歴]
//
// (初版作成)
//   設計         2020/08/01  承認：      調査：     担当：（福技）三木
//   製造             /  /            承認：      調査：     担当：（福技）ハイ
//   単体試験      /  /             承認：      調査：     担当：（福技）ハイ
//
// Rev.               /  /     承認：      調査：     担当：
//===============================================================================
//  Version     変更日            変更者              変更内容
 *
 *  Rev.q001               /  /     承認：      調査：     担当： VuQT
 *  変更理由
 *  2022/06/02         (TSDV) VuQT                東北向けの修正
*/

package eccm.dto;

import java.io.Serializable;
import java.util.List;

import eccm.entity.CcmJob;
import eccm.entity.CcmSsConstruction;
import eccm.entity.CcmStationMaster;
import eccm.entity.EccmCJob;
import eccm.entity.WorkDrawingData;

public class JobListData implements Serializable {

	private static final long serialVersionUID = 1L;

	public List<CcmJob> jobs;
	
	public List<EccmCJob> eccmcjobs; // Rev.q001T

	public List<WorkDrawingData> evidences;
	
	public List<CcmStationMaster> stations;

	public List<CcmSsConstruction> constructions;

}
