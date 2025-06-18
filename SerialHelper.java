package com.hyosung.tnsplm.serial.service;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
//import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

//import org.apache.ibatis.session.SqlSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;

import com.hyosung.tnsplm.common.utils.CommonUtils;
import com.hyosung.tnsplm.common.utils.IBAUtils;
import com.hyosung.tnsplm.epm.service.EPMHelper;
import com.hyosung.tnsplm.masterData.entity.ClassHeader;
import com.hyosung.tnsplm.masterData.entity.ClassItem;
import com.hyosung.tnsplm.masterData.entity.ObjectClassificationMapping;
import com.hyosung.tnsplm.masterData.entity.Stringvalue2;
import com.hyosung.tnsplm.masterData.entity.Stringvalue3;
import com.hyosung.tnsplm.masterData.entity.ZMdmClassMaterialValue;
//import com.hyosung.tnsplm.mybatis.config.util.tnsSessionManager;
import com.hyosung.tnsplm.part.service.PartHelper;
//import com.hyosung.tnsplm.serial.dao.serialDao;
import com.hyosung.tnsplm.restAPI.service.RestAPIHelper;
import com.hyosung.tnsplm.serial.entity.SerialList;
import com.hyosung.tnsplm.serial.entity.SerialListMapping;
import com.hyosung.tnsplm.serial.entity.WTPartSerialList;

import wt.clients.vc.CheckInOutTaskLogic;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentHelper;
import wt.epm.EPMDocumentMaster;
import wt.epm.EPMDocumentMasterIdentity;
import wt.epm.build.EPMBuildHistory;
import wt.epm.build.EPMBuildRule;
import wt.fc.IdentityHelper;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTKeyedHashMap;
import wt.fc.collections.WTKeyedMap;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.value.BooleanValue;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.FloatValue;
import wt.iba.value.IBAHolder;
import wt.iba.value.StringValue;
import wt.iba.value.TimestampValue;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.litevalue.BooleanValueDefaultView;
import wt.iba.value.litevalue.FloatValueDefaultView;
import wt.iba.value.litevalue.StringValueDefaultView;
import wt.iba.value.litevalue.TimestampValueDefaultView;
import wt.iba.value.service.IBAValueHelper;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.ixb.handlers.forattributes.IBAValues;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleTemplateReference;
import wt.lifecycle.State;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.ownership.Ownership;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.part.WTPartTypeInfo;
import wt.pom.Transaction;
import wt.query.ClassAttribute;
import wt.query.OrderBy;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.services.ServiceFactory;
import wt.session.SessionHelper;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.VersionControlHelper;
import wt.vc.views.View;
import wt.vc.views.ViewHelper;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressHelper;

/**
 * @클래스명 : SerialHelper.java
 * @최초 작성자 : jymin
 * @최초 작성일 : 2025. 02. 25
 * @설명 : sql
 */
public class SerialHelper {

	public static final SerialService service = ServiceFactory.getService(SerialService.class);
	public static final SerialHelper manager = new SerialHelper();
	public static final java.time.ZonedDateTime koreaZonedDateTime = java.time.ZonedDateTime
			.now(java.time.ZoneId.of("Asia/Seoul"));
	public static final Timestamp timestamp = Timestamp.valueOf(koreaZonedDateTime.toLocalDateTime());

	public static SimpleDateFormat dateSdf = null;
	public static SimpleDateFormat timeSdf = null;
	private static DecimalFormat df = null;
    // 상수 정의
    private static final int MAX_NAME_LENGTH = 26;
    private static final int MAX_FIELD_LENGTH = 100;
    private static final String LIFECYCLE_STATE_RELEASED = "CR";

    private static final String ERROR_MATNR = "ERROR";
    private static final String DEFAULT_NAME = "notName";
    private static final String DEFAULT_FOLDER_PATH = "/Default";
    private static final String LIFECYCLE_TEMPLATE = "TNS_STANDARD";
    private static final String VIEW_NAME = "M-BOM";
    private static final int MAX_NAME_LENGTH1 = 30;
    private static final String DESC_FLAG_VALUE = "X";
    
    // 제거할 속성 키 목록
    private static final Set<String> EXCLUDED_ATTRIBUTES = Set.of(
        "partSerialListOID", "serialListOID", "partOID", 
        "Class", "serialListMappingOID", "epmDocumentOID"
    );
    
	static {
		dateSdf = new SimpleDateFormat("yyyyMMdd");
		timeSdf = new SimpleDateFormat("HHmmss");
		df = new DecimalFormat("0000");
	}

	// 상태
	private static final String WIP = "WIP";
	private static final String CR = "CR";

	// ------------------------------------------------------------------------------------------------
	// [채번 리스트] 조회
	//
	public List<Map<String, Object>> getSerialList(Map<String, Object> params) throws Exception {
		List<Map<String, Object>> resultList = new ArrayList<>();

		// WTPrincipal 가져오기 (현재 사용자)
		WTPrincipal prin = SessionHelper.manager.getPrincipal();
		System.out.println("SSSSS: " + params);
		try {
			Class[] getClass = new Class[] { WTPartSerialList.class, WTPart.class, WTPartMaster.class }; // ,
																											// StringValue.class
			Class[] getSerialClass = new Class[] { SerialListMapping.class, SerialList.class };

			// -----------------------------------------------------------
			Class cls_serial = WTPartSerialList.class;
			// QuerySpec 생성
			//
			QuerySpec query;
			query = new QuerySpec();
			// query.setAdvancedQueryEnabled(true);

			// -----------------------------------------------------------
			// QuerySpec 조인
			//
			int idx = query.appendClassList(cls_serial, true);
			int idx1 = query.appendClassList(getClass[1], true);
			int idx2 = query.appendClassList(getClass[2], true);
			int idx3 = query.appendClassList(getSerialClass[0], true);
			int idx4 = query.appendClassList(getSerialClass[1], true);
			// int idx3 = query.appendClassList(getClass[3], true);

			// A0.H2_WTPART_MST_VRID = A1.branchIditerationInfo
			SearchCondition cond1 = new SearchCondition(
					new ClassAttribute(cls_serial, WTPartSerialList.H2__WTPART__MST__VRID), SearchCondition.EQUAL,
					new ClassAttribute(WTPart.class, "iterationInfo.branchId"));
			SearchCondition cond2 = new SearchCondition(getClass[1], "iterationInfo.latest", SearchCondition.IS_TRUE,
					true);
			SearchCondition cond5 = new SearchCondition(getClass[1], "iterationInfo.latest", SearchCondition.IS_FALSE,
					true);
			SearchCondition cond3 = new SearchCondition(getClass[1], "checkoutInfo.state",
					new String[] { "c/i", "c/o" }, true, false);
			SearchCondition state1 = new SearchCondition(getClass[1], "state.state", new String[] { "WIP" }, true,
					false); // , "NUM" , "CR"

			// OR

			SearchCondition cond4 = new SearchCondition(
					new ClassAttribute(cls_serial, WTPartSerialList.H2__WTPART__MST__OID), SearchCondition.EQUAL,
					new ClassAttribute(getClass[1], "thePersistInfo.theObjectIdentifier.id"));
			SearchCondition state2 = new SearchCondition(getClass[1], "state.state", new String[] { "NUM", "CR" }, true,
					false); // "WIP",

			// OrCondition
			// AndCondition

			query.appendOpenParen();

			query.appendOpenParen();

			query.appendWhere(cond1, new int[] { idx, idx1 });
			query.appendAnd();
			query.appendWhere(cond2, new int[] { idx1 });
			query.appendAnd();
			query.appendWhere(cond3, new int[] { idx1 });
			query.appendAnd();
			query.appendWhere(state1, new int[] { idx1 });

			query.appendCloseParen();

			query.appendOr();

			query.appendOpenParen();

			query.appendWhere(cond4, new int[] { idx, idx1 });
			query.appendAnd();
//		query.appendWhere(cond5, new int[] { idx1 });
//		query.appendAnd();
			query.appendWhere(state2, new int[] { idx1 });

			query.appendCloseParen();

			query.appendCloseParen();

			// WTPartSerialList , WTPart
//			SearchCondition condition17 = new SearchCondition(
//						getClass[0] , WTPartSerialList.H2__WTPART__MST__OID, 
//						getClass[1], "thePersistInfo.theObjectIdentifier.id"
//						);                                    
//			condition17.setOuterJoin(SearchCondition.NO_OUTER_JOIN);
//			query.appendWhere(condition17, new int[] { idx,
//			idx1 });
//	

			query.appendAnd();

			// WTPart, WTPartMaster
			SearchCondition condition18 = new SearchCondition(getClass[1], "masterReference.key.id", // ida3masterreference
					getClass[2], "thePersistInfo.theObjectIdentifier.id" // ida2a2
			);
			condition18.setOuterJoin(SearchCondition.NO_OUTER_JOIN);
			query.appendWhere(condition18, new int[] { idx1, idx2 });

			query.appendAnd();

			// WTPartSerialList, SerialListMapping
			SearchCondition condition19 = new SearchCondition(getClass[0], "thePersistInfo.theObjectIdentifier.id", // ida2a2
					getSerialClass[0], "H2_WTPARTSERIALLIST_OID" // 자재 채번 대상 목록 oid
			);
			condition19.setOuterJoin(SearchCondition.RIGHT_OUTER_JOIN);
			query.appendWhere(condition19, new int[] { idx, idx3 });

			query.appendAnd();

			// SerialListMapping, SerialList
			SearchCondition condition20 = new SearchCondition(getSerialClass[0], "H2_SERIALLIST_OID", // 채번 승인 요청 정보 oid
					getSerialClass[1], "thePersistInfo.theObjectIdentifier.id" // ida2a2
			);
			condition20.setOuterJoin(SearchCondition.RIGHT_OUTER_JOIN);
			query.appendWhere(condition20, new int[] { idx3, idx4 });

			// prin.getName()
			System.out.println("prin Name : " + prin.getName());
			if (prin != null && (!prin.getName().contains("Administrator") || !prin.getName().contains("wcadmin"))) {
				query.appendAnd();
				query.appendWhere(
						new SearchCondition(getClass[0], "H2_REG_USER", SearchCondition.EQUAL, prin.getName()),
						new int[] { idx });
			}

			String jtc1 = Objects.toString(params.get("JsonTypeCode1"), "전체");
			String jtc2 = Objects.toString(params.get("JsonTypeCode2"), "전체");

			if (!jtc1.equals("전체")) {
				query.appendAnd();
				query.appendWhere(new SearchCondition(getSerialClass[1], "H2_REQ_STATUS", SearchCondition.EQUAL, jtc1),
						new int[] { idx4 });
			}

			if (!jtc2.equals("전체")) {
				query.appendAnd();
				query.appendWhere(new SearchCondition(getClass[0], "H2_TARGET_STATUS", SearchCondition.EQUAL, jtc2),
						new int[] { idx });
			} else {
				// 채번 완료시 채번완료 제외 로직 주석
//				query.appendAnd();
//				query.appendWhere(new SearchCondition(
//						getClass[0], "H2_TARGET_STATUS",
//						SearchCondition.IS_NULL,
//						true
//		        	), new int[] { idx } );
			}

			// query.appendOrderBy(new OrderBy(new ClassAttribute(EPMDocumentMaster.class,
			// EPMDocumentMaster.CADNAME), true), new int[]{idxDocMaster});

			/*
			 * query.appendAnd(); query.appendWhere(new
			 * SearchCondition(WTPart.class,Iterated.LATEST_ITERATION,
			 * SearchCondition.IS_TRUE), new int[]{idx1});
			 */

			// CommonUtils.addLastVersionCondition2(query, WTPart.class, idx1);

			// -----------------------------------------------------------
			// 데이터 조회 실행
			//

			System.out.println("" + query.toString());
			QueryResult results = PersistenceHelper.manager.find(query);

			// --------------------------------------------------------------
			// results
			//
			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				Map<String, Object> dataMap = new HashMap<>();
				WTPartSerialList wpsl = (WTPartSerialList) obj[0];
				WTPart wp = (WTPart) obj[1];
				WTPartMaster wpm = (WTPartMaster) obj[2];
				SerialListMapping sm = (SerialListMapping) obj[3];
				SerialList sl = (SerialList) obj[4];
				EPMDocument ed = getEPMDocumentByWTPart(wp);
				// -------------------------------------------------------
				// 해당 클래스의 필드 조회
				//
				String FileName = "";
				String ApprovalNumber = "";
				String H2_REQ_STATUS = "";
				String epmDocumentOID = "";
				String serialListOID = "";
				String serialListMappingOID = "";
				if (sl != null) {
					ApprovalNumber = String.valueOf(sl.getH2_ITEM_REQ_ID());
					H2_REQ_STATUS = String.valueOf(sl.getH2_REQ_STATUS());
					serialListOID = sl.getPersistInfo().getObjectIdentifier().getStringValue();
				}

				if (ed != null) {
					FileName = ed.getCADName();
					epmDocumentOID = ed.getPersistInfo().getObjectIdentifier().getStringValue();
				}

				if (sm != null) {
					serialListMappingOID = sm.getPersistInfo().getObjectIdentifier().getStringValue();
				}
				String outClass = Objects.toString(wp.getTypeInfoWTPart().getPtc_str_1(), "");
				ClassHeader ch = null;
				if (!outClass.equals("")) {
					ch = getFindClassHeader(outClass);
				}
				String IS3S = getFindClassHeaderIS3S(wp.getTypeInfoWTPart().getPtc_str_1());
				dataMap.put("IS3S", IS3S); // 클래스 미결재, 결재 여부
				dataMap.put("CHECK_REQ", "X"); // 점검모드
				dataMap.put("partOID", wp.getPersistInfo().getObjectIdentifier().getStringValue()); // partOid
				dataMap.put("epmDocumentOID", epmDocumentOID); // epmDocumentOID
				dataMap.put("partSerialListOID", wpsl.getPersistInfo().getObjectIdentifier().getStringValue()); // partSerialListOID
				dataMap.put("serialListOID", serialListOID); // serialListOID
				dataMap.put("serialListMappingOID", serialListMappingOID); // serialListMappingOID
				dataMap.put("ApprovalNumber", ApprovalNumber); // 채번 승인요청번호
				dataMap.put("H2_REQ_STATUS", H2_REQ_STATUS); // 채번대상 승인요청 상태
				// dataMap.put("Thumbnail",wp.getPersistInfo().getObjectIdentifier().getStringValue());
				// //썸네일
				// dataMap.put("BOM",wp.getPersistInfo().getObjectIdentifier().getStringValue());
				// // BOM
				dataMap.put("FileName", FileName); // 파일명
				dataMap.put("name", wpm.getName()); // 품명
				dataMap.put("number", wpm.getNumber()); // 품번
				dataMap.put("ver", wp.getVersionIdentifier().getValue() + "." + wp.getIterationIdentifier().getValue()); // 버전
				dataMap.put("H2_TARGET_STATUS", wpsl.getH2_TARGET_STATUS()); // 채번 품목상태
				dataMap.put("TITLE", IBAUtils.getStringValue(wp, "TITLE")); // TITLE 매개변수 속성 , 값 TITLE
				dataMap.put("ClassName", ch != null ? ch.getH2_KSCHL() : outClass); // Classification(분류체계) 속성 값
				dataMap.put("Class", outClass); // Classification(분류체계) 속성 값
				dataMap.put("HIERARCHY", IBAUtils.getStringValue(wp, "HIERARCHY")); // Herarchy 속성 값 Hierarchy
				dataMap.put("HR", IBAUtils.getStringValue(wp, "HR")); // 형상기호 속성 값 HR
				dataMap.put("MATERIAL", IBAUtils.getStringValue(wp, "MATERIAL")); // 재질 속성 값 Material
				dataMap.put("PC", IBAUtils.getStringValue(wp, "PC")); // 표면처리 속성 값 PC
				dataMap.put("H2_REG_USER", wpsl.getH2_REG_USER()); // 작성자
				dataMap.put("Message", wpsl.getH2_IF_MESSAGE()); // 메시지
				dataMap.put("Success", wpsl.getH2_IF_SUCCESS_DIV()); // 검증
				// 검토자
				// 승인자
				Date nowDate = timestamp;
				Date getDate = wpsl.getH2_TARGET_REGISTER_DATE();

				if (getDate != null) {
					String checkStatus = Objects.toString(wpsl.getH2_TARGET_STATUS(), "");//
					String nowLocalDate = dateSdf.format(nowDate);
					String getLocalDate = dateSdf.format(getDate);

					if (nowLocalDate.equals(getLocalDate) && checkStatus.equals("채번완료")) {
						dataMap.put("Color", "Y");
					} else {
						dataMap.put("Color", "N");
					}
				} else {
					dataMap.put("Color", "N");
				}

				resultList.add(dataMap);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return sortByFileName(resultList);
	}

	public static List<Map<String, Object>> sortByFileName(List<Map<String, Object>> resultList) {
		List<Map<String, Object>> sortedList = new ArrayList<>(resultList);

		sortedList.sort(new java.util.Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> map1, Map<String, Object> map2) {

				String color1 = (String) map1.get("Color");
				String color2 = (String) map2.get("Color");

				if ("Y".equals(color1) && !"Y".equals(color2)) {
					return -1; // map1이 앞
				}
				if (!"Y".equals(color1) && "Y".equals(color2)) {
					return 1; // map2가 앞
				}

				String fileName1 = (String) map1.get("FileName");
				String fileName2 = (String) map2.get("FileName");

				if ("".equals(fileName1) && !"".equals(fileName2)) {
					return 1; // fileName1이 빈 값이면 뒤로
				}
				if (!"".equals(fileName1) && "".equals(fileName2)) {
					return -1; // fileName2가 빈 값이면 뒤로
				}

				if (fileName1 == null && fileName2 != null) {
					return 1; // fileName1이 null이면 뒤로
				}
				if (fileName1 != null && fileName2 == null) {
					return -1; // fileName2가 null이면 뒤로
				}

				return fileName1.compareTo(fileName2);
			}
		});

		return sortedList;
	}

	// getPartList
	public Map<String, Object> getWTPartListWithEPMDocument(Map<String, Object> params) throws Exception {
		Map<String, Object> map = new HashMap<>();
		ArrayList<Map<String, Object>> list = new ArrayList<>();
		// List<Map<String,Object>> datas = new ArrayList<>();
		try {
			QuerySpec query = new QuerySpec();
			int idxPart = query.appendClassList(WTPart.class, true);
			int idxPartMaster = query.appendClassList(WTPartMaster.class, false);
			int idxBuildRule = query.appendClassList(EPMBuildRule.class, false);
			int idxDoc = query.appendClassList(EPMDocument.class, true);
			int idxDocMaster = query.appendClassList(EPMDocumentMaster.class, true);
			int idxUser = query.appendClassList(WTUser.class, false);

			query.setAdvancedQueryEnabled(true);

			// 조인 조건들
			query.appendWhere(new SearchCondition(WTPart.class, "masterReference.key.id", WTPartMaster.class,
					"thePersistInfo.theObjectIdentifier.id"), idxPart, idxPartMaster);
			query.appendAnd();
			query.appendWhere(new SearchCondition(WTPart.class, "iterationInfo.branchId", EPMBuildRule.class,
					"roleBObjectRef.key.branchId"), idxPart, idxBuildRule);
			query.appendAnd();
			query.appendWhere(new SearchCondition(EPMBuildRule.class, "roleAObjectRef.key.branchId", EPMDocument.class,
					"iterationInfo.branchId"), idxBuildRule, idxDoc);
			query.appendAnd();
			query.appendWhere(new SearchCondition(EPMDocument.class, "masterReference.key.id", EPMDocumentMaster.class,
					"thePersistInfo.theObjectIdentifier.id"), idxDoc, idxDocMaster);
			query.appendAnd();
			query.appendWhere(new SearchCondition(EPMDocument.class, "iterationInfo.creator.key.id", WTUser.class,
					"thePersistInfo.theObjectIdentifier.id"), idxDoc, idxUser);
			query.appendAnd();

			query.appendWhere(new SearchCondition(EPMDocument.class, "checkoutInfo.state",
					new String[] { "c/i", "c/o" }, true, false), new int[] { idxDoc });
			query.appendAnd();
			query.appendWhere(
					new SearchCondition(EPMDocument.class, "state.state", new String[] { "WIP" }, true, false),
					new int[] { idxDoc });
			query.appendAnd();
			query.appendWhere(
					new SearchCondition(WTPart.class, "checkoutInfo.state", new String[] { "c/i", "c/o" }, true, false),
					new int[] { idxPart });
			query.appendAnd();
			query.appendWhere(new SearchCondition(WTPart.class, "state.state", new String[] { "WIP" }, true, false),
					new int[] { idxPart });
			query.appendAnd();

			// 조건 필터 처리 (기존 epmListWhere 내용을 여기 삽입)
			epmListWhere(query, params, idxPart, idxPartMaster, idxBuildRule, idxDoc, idxDocMaster, idxUser);

			// Select 대상 컬럼 명시
//		        query.appendSelect(new ClassAttribute(WTPart.class, "thePersistInfo.theObjectIdentifier.classname"), new int[]{idxPart}, true);
//		        query.appendSelect(new ClassAttribute(WTPart.class, "thePersistInfo.theObjectIdentifier.id"), new int[]{idxPart}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocument.class, "thePersistInfo.theObjectIdentifier.classname"), new int[]{idxDoc}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocument.class, "thePersistInfo.theObjectIdentifier.id"), new int[]{idxDoc}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocument.class, "versionInfo.identifier.versionId"), new int[]{idxDoc}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocument.class, "iterationInfo.identifier.iterationId"), new int[]{idxDoc}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocument.class, "state.state"), new int[]{idxDoc}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocument.class, EPMDocument.CREATE_TIMESTAMP), new int[]{idxDoc}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocumentMaster.class, "thePersistInfo.theObjectIdentifier.classname"), new int[]{idxDocMaster}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocumentMaster.class, "thePersistInfo.theObjectIdentifier.id"), new int[]{idxDocMaster}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocumentMaster.class, EPMDocumentMaster.NAME), new int[]{idxDocMaster}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocumentMaster.class, EPMDocumentMaster.NUMBER), new int[]{idxDocMaster}, true);
//		        query.appendSelect(new ClassAttribute(EPMDocumentMaster.class, EPMDocumentMaster.CADNAME), new int[]{idxDocMaster}, true);

			// 정렬 조건
			query.appendOrderBy(
					new OrderBy(new ClassAttribute(EPMDocumentMaster.class, EPMDocumentMaster.CADNAME), true),
					new int[] { idxDocMaster });

//		        newPageQueryUtils pager = new newPageQueryUtils(params, query);
//		        PagingQueryResult results = pager.find();
			System.out.println("query::: " + query.toString());
			QueryResult results = PersistenceHelper.manager.find(query);

			while (results.hasMoreElements()) {
				Object[] obj = (Object[]) results.nextElement();
				int i = 0;
				Map<String, Object> data = new HashMap<>();
				System.out.println("obj " + obj);
				WTPart part = (WTPart) obj[0];
				EPMDocument epm = (EPMDocument) obj[1];
				EPMDocumentMaster epmMaster = (EPMDocumentMaster) obj[2];

				data.putAll(Map.of("name", epmMaster.getName(), "number", epmMaster.getNumber(), "TITLE",
						IBAUtils.getStringValue(part, "TITLE"), "partOID",
						part.getPersistInfo().getObjectIdentifier().getStringValue(), "epmOID",
						epm.getPersistInfo().getObjectIdentifier().getStringValue(), "epmMasterOID",
						epmMaster.getPersistInfo().getObjectIdentifier().getStringValue(), "ver",
						epm.getVersionIdentifier().getValue() + "." + epm.getIterationIdentifier().getValue(), "status",
						epm.getLifeCycleState().getDisplay(SessionHelper.manager.getLocale()), "cadName",
						epmMaster.getCADName()));
				data.put("Created", epm.getCreatorFullName());
				data.put("CreatedDate", dateSdf.format(epm.getCreateTimestamp()));
				list.add(data);

//		            String partOID = obj[i++] + ":" + obj[i++];
//		            String epmOID = obj[i++] + ":" + obj[i++];
//		            String version = (String) obj[i++];
//		            String iteration = (String) obj[i++];
//		            String status = (String) obj[i++];
//		            Timestamp createdDate = (Timestamp) obj[i++];
//		            String epmMasterOID = obj[i++] + ":" + obj[i++];
//		            String name = (String) obj[i++];
//		            String number = (String) obj[i++];
//		            String cadName = (String) obj[i++];
//
//		            data.put("partOID", partOID);
//		            data.put("epmOID", epmOID);
//		            data.put("epmMasterOID", epmMasterOID);
//		            data.put("ver", version + "." + iteration);
//		            data.put("status", status);
//		            data.put("CreatedDate", dateSdf.format(createdDate));
//		            data.put("Created", ""); // 추후 필요 시 추가
//		            data.put("name", name);
//		            data.put("number", number);
//		            data.put("cadName", cadName);

				// list.add(data);
			}

			map.putAll(Map.of("Data", list/*
											 * , "topListCount", pager.getTotal(), "pageSize", pager.getPsize(),
											 * "total", pager.getTotalSize(), "curPage", pager.getCpage()
											 */
			));

		}

		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return map;
	}

	// ---------------------------------------------------------------------------
	// [임시] EPMDocument 객체 갖고오기
	//
	//
	public EPMDocument getEPMDocumentByWTPart(WTPart part) throws Exception {
		EPMDocument epm = null;
		try {
			if (part == null) {
				return epm;
			}
			QueryResult result = null;
			if (VersionControlHelper.isLatestIteration(part)) {
				result = PersistenceHelper.manager.navigate(part, "buildSource", EPMBuildRule.class);
			} else {
				result = PersistenceHelper.manager.navigate(part, "builtBy", EPMBuildHistory.class);
			}
			while (result.hasMoreElements()) {
				EPMDocument epmDoc = (EPMDocument) result.nextElement();
				return epmDoc;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------------
	// [채번 리스트] 수정 데이터 조회
	// [TODO] Find 속성 찾기 ok 2025.03.18
	//
	public Map<String, Object> getWTPartToEdit(Map<String, Object> param) throws Exception {
		try {
			Map<String, Object> output = new HashMap<String, Object>();
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			// M<S,M<S,O>> -> M<S,O> casting
			for (Object obj : param.values()) {
				if (obj instanceof Map) {
					Map<String, Object> input = (Map<String, Object>) obj;
					output.putAll(input);
				}
			}

			Map<String, Object> response = new HashMap<String, Object>();
			String partOid = Objects.toString(output.get("partOID"), "");
			String Class = Objects.toString(output.get("Class"), "");
			String partSerialListOid = Objects.toString(output.get("partSerialListOID"), "");
			String epmDocumentOid = Objects.toString(output.get("epmDocumentOID"), "");
			String serialListOid = Objects.toString(output.get("serialListOID"), "");
			String serialListMappingOid = Objects.toString(output.get("serialListMappingOID"), "");

			QuerySpec query;
			query = new QuerySpec();
			QueryResult results = null;
			List<ClassItem> clWhere = new ArrayList<>();
			List<ObjectClassificationMapping> ocmWhere = new ArrayList<>();

			WTPart wp = null;
			WTPartMaster wpm = null;

			ReferenceFactory rf = new ReferenceFactory();
			Persistable persistable = null;

			if (!partOid.isEmpty() || !partOid.isBlank()) {
				persistable = rf.getReference(partOid).getObject();
				if (persistable instanceof WTPart) {
					wp = (WTPart) persistable;
					wpm = wp.getMaster();
					persistable = null;
				} else {
					throw new Exception("선택된 자재 찾기 불가로 수정시 새로운 자재가 생성 됩니다.");
				}
			} else {
				throw new Exception("선택된 자재 찾기 불가로 수정시 새로운 자재가 생성 됩니다.");
			}

			int idx = query.appendClassList(ClassItem.class, true);
			query.appendWhere(new SearchCondition(ClassItem.class, ClassItem.H2__CLASS, SearchCondition.EQUAL, Class),
					idx);

			query.appendOr();
			// 제너릭 데이터 불러오기
			query.appendWhere(new SearchCondition(ClassItem.class, ClassItem.H2__CLASS, SearchCondition.EQUAL, "GENG"),
					idx);

			results = PersistenceHelper.manager.find(query);

			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ClassItem cl = (ClassItem) obj[0];
				clWhere.add(cl // (ClassItem) obj[0]
				);
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("H2_FIELDPROP", cl.getH2_FIELDPROP());
				map.put("H2_ATBEZ", cl.getH2_ATBEZ());
				list.add(map);
			}

			// ------------------------------------------------------------------------
			// [TODO]
			// 속성 삭제 후 재 매핑이 가능해지는 경우
			// ObjectClassificationMapping는 날짜 및 h2_atbez로 찾는다

			query = new QuerySpec();
			results = null;

			int idx1 = query.appendClassList(ObjectClassificationMapping.class, true);
			int size = clWhere.size(); // 조건의 총 개수
			int count = 1;

			// ------------------------------------------------------------------------
			// 조회 CLASS가 없는 경우
			//
			if (size == 0) {
				throw new Exception("분류체계 [CLASS] 찾기 불가로 다른 분류체계를 선택 하십시오.");
			}

			for (ClassItem data : clWhere) {
				query.appendWhere(new SearchCondition(ObjectClassificationMapping.class,
						ObjectClassificationMapping.H2__ATNAM, SearchCondition.EQUAL, data.getH2_ATNAM()), idx1);

				if (count == size) {
					break;
				}
				count++;
				query.appendOr();

			}
			System.out.println("query " + query.toString());
			results = PersistenceHelper.manager.find(query);

			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ocmWhere.add((ObjectClassificationMapping) obj[0]);
			}
			// ------------------------------------------------------------------------
			// 조회 매핑 속성이 없는 경우
			//
			size = ocmWhere.size();
			if (size == 0) {
				throw new Exception("매핑된 속성 찾기 불가");
			}

			// ------------------------------------------------------------------------
			// [Find] 속성 찾기
			//
			for (ObjectClassificationMapping data : ocmWhere) {
				if ("GR".equals(data.getH2_PROP_TYPE())) {
//            	    String stringValue = IBAUtils.getStringValue(wp, data.getH2_PLM_PROP());
//            	    if (stringValue != null && !stringValue.trim().isEmpty()) {
//            	        response.put(data.getH2_PLM_PROP(), stringValue.trim());
//            	    } else {
//            	        float floatValue = IBAUtils.getFloatValue(wp, data.getH2_PLM_PROP());
//            	        if(floatValue != 0f) {
//            	        	response.put(data.getH2_PLM_PROP(), floatValue);
//            	        }else {
//            	        	boolean booleanValue = IBAUtils.getBooleanValue(wp, data.getH2_PLM_PROP());
//            	        	response.put(data.getH2_PLM_PROP(), booleanValue);
//            	        }
//            	    }
//            	    
					String key = data.getH2_PLM_PROP();
					System.out.println("GR key:: " + key);

					String stringValue = IBAUtils.getStringValue(wp, key);
					if (stringValue != null && !stringValue.trim().isEmpty()) {
						response.put(key, stringValue.trim());
					} else {
						float floatValue = IBAUtils.getFloatValue(wp, key);
						if (floatValue != 0f) {
							response.put(key, floatValue);
						} else {
							System.out.println("booleansss");
							Boolean booleanValue = IBAUtils.getBooleanValue(wp, key);
							if (booleanValue != null) {
								response.put(key, booleanValue);
							}
						}
					}

				} else if ("LR".equals(data.getH2_PROP_TYPE())) {
					response.put(data.getH2_PLM_PROP(), IBAUtils.getStringValue(wp, data.getH2_PLM_PROP()));
				}
				// else if("LR".equals(data.getH2_PROP_TYPE())) { response.put("LR:" +
				// data.getH2_PLM_PROP(), getStringValues(true, data, wp, wpm)); }
				else if ("X".equals(data.getH2_PROP_TYPE())) {
					response.put(data.getH2_PLM_PROP(), getStringValues(false, data, wp, wpm));
				}
				// else { throw new Exception("매핑 속성 타입이 없는 데이터가 존재 합니다.[GR, LR, X] : " +
				// data.getH2_PLM_PROP() ); }
				else {
					throw new Exception("message: 매핑 속성 타입이 없는 데이터가 존재 합니다.[GR, LR, X]");
				}

//            	map.put("MAPPING_H2_PLM_PROP", ocm != null ? ocm.getH2_PLM_PROP() : "NOT");
//				map.put("MAPPING_H2_PROP_TYPE", ocm != null ? ocm.getH2_PROP_TYPE() : "NOT");
			}

//			response.put("H2_CLASS", data.getH2_CLASS());	// 클래스
//			response.put("H2_ATNAM", data.getH2_ATNAM());	// 특성
//			response.put("H2_ZGROUP", data.getH2_ZGROUP());	 // 특성그룹 
//			response.put("H2_ATBEZ", data.getH2_ATBEZ());	// 특성명
//			response.put("H2_ATFOR", data.getH2_ATFOR());	// 데이터유형
//			response.put("H2_ANZST", data.getH2_ANZST());	// 길이 
//			response.put("H2_ANZDZ", data.getH2_ANZDZ());// 소수자리
//			response.put("H2_IPTYPE",  data.getH2_IPTYPE());// 입력방식
//			response.put("H2_ATKLE", data.getH2_ATKLE()); // 대소문자구분
//			response.put("H2_ATAME", data.getH2_ATAME()); // 다중값 허용
//			response.put("H2_ATINT",data.getH2_ATINT()); // 범위값 허용
//			response.put("H2_CD_CLASS",data.getH2_CD_CLASS()); // 특성값 공통코드ID
//			response.put("H2_FIELDPROP",data.getH2_FIELDPROP()); // 필수속성
//			response.put("H2_PREFIX", data.getH2_PREFIX()); // 자재내역 prifix
//			response.put("H2_SUFFIX", data.getH2_SUFFIX()); // 자재내역 suffix
//			response.put("H2_ZORDER", data.getH2_ZORDER());	//정렬순서
//			response.put("H2_DESC_FLAG", data.getH2_DESC_FLAG()); // 자재내역 조합 대상
//			response.put("H2_DESC_ORDER", data.getH2_DESC_ORDER()); // 자재내역 조합 순서

			response.put("Class", Class);
			if (!response.containsKey("TITLE") || response.get("TITLE") == null
					|| response.get("TITLE").toString().trim().isEmpty()) {
				ClassHeader ch = getFindClassHeader(Class);
				response.put("TITLE", ch != null ? ch.getH2_PREFIX_DESC() : "");
			}
			if (!response.containsKey("HIERARCHY") || response.get("HIERARCHY") == null
					|| response.get("HIERARCHY").toString().trim().isEmpty()) {
				response.put("HIERARCHY", getFindZmdmClassMaterialValue(Class));
			}
			// dataMap.put("HIERARCHY",IBAUtils.getStringValue(wp, "HIERARCHY"));
			response.put("partOID", partOid);
			response.put("partSerialListOID", partSerialListOid);
			response.put("epmDocumentOID", epmDocumentOid);
			response.put("serialListOID", serialListOid);
			response.put("serialListMappingOID", serialListMappingOid);
			// response.put("list", list);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;

		}
	}

	// ------------------------------------------------------------------------------------------------
	// [nonCad] 분류체계 별 List
	//
	public List<Map<String, Object>> getAttributesByClass(String Class) throws Exception {
		// String[] atbez = {"PartName","PartNo", "Material", "HR", "Hierarchy", "PC",
		// "TITLE"};
		// for(int i=1; i<=7; i++) {
		// Map<String,Object> map = new HashMap<>();
		// map.put("H2_CLASS", "CL00000003"); // 클래스
		// map.put("H2_ATNAM", "C000" + i); // 특성
		// map.put("H2_ZGROUP", ""); // 특성그룹
		// map.put("H2_ATBEZ", atbez[i - 1]); // 특성명
		// map.put("H2_ATFOR", i == 4 ? "NUM" : i == 6 ? "NUM" : "CHAR"); // 데이터유형
		// map.put("H2_ANZST", "30"); // 길이
		// map.put("H2_ANZDZ", "");// 소수자리
		// map.put("H2_IPTYPE", "T");// 입력방식
		// map.put("H2_ATKLE", i == 1 ? "" : i == 3 ? "" : "X"); // 대소문자구분
		// map.put("H2_ATAME", i == 3 ? "X" : i == 5 ? "X" : ""); // 다중값 허용
		// map.put("H2_ATINT",""); // 범위값 허용
		// map.put("H2_CD_CLASS",""); // 특성값 공통코드ID
		// map.put("H2_FIELDPROP",""); // 필수속성
		// map.put("H2_PREFIX", ""); // 자재내역 prifix
		// map.put("H2_SUFFIX", ""); // 자재내역 suffix
		// map.put("H2_ZORDER", i); //정렬순서
		// map.put("H2_DESC_FLAG", "X"); // 자재내역 조합 대상
		// map.put("H2_DESC_ORDER", i); // 자재내역 조합 순서
		// list.add(map);
		// }
		// model.addObject("datas", list);
		try {
			if (Class == null) {
				return null;
			}

			QuerySpec query;
			query = new QuerySpec();
			QueryResult results = null;
			List<Map<String, Object>> clWhere = new ArrayList<>();
			int idx = query.appendClassList(ClassItem.class, true);
			int idx1 = query.appendClassList(ObjectClassificationMapping.class, true);

			SearchCondition condition19 = new SearchCondition(ClassItem.class, ClassItem.H2__ATNAM,
					ObjectClassificationMapping.class, ObjectClassificationMapping.H2__ATNAM);
			condition19.setOuterJoin(SearchCondition.RIGHT_OUTER_JOIN);
			query.appendWhere(condition19, new int[] { idx, idx1 });

			query.appendAnd();

			query.appendWhere(new SearchCondition(ClassItem.class, ClassItem.H2__CLASS, SearchCondition.EQUAL, Class),
					idx);
			System.out.println("" + query.toString());

			ClassAttribute ca = new ClassAttribute(ClassItem.class, ClassItem.H2__DESC__ORDER);
			query.appendOrderBy(new OrderBy(ca, false), new int[] { idx });

			results = PersistenceHelper.manager.find(query);

			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ClassItem cl = (ClassItem) obj[0];
				ObjectClassificationMapping ocm = (ObjectClassificationMapping) obj[1];
				Map<String, Object> map = new HashMap<>();
				map.put("MAPPING_H2_PLM_PROP", ocm != null ? ocm.getH2_PLM_PROP() : "NOT");
				map.put("MAPPING_H2_PROP_TYPE", ocm != null ? ocm.getH2_PROP_TYPE() : "NOT");
				map.put("MAPPING_OID",
						ocm != null ? ocm.getPersistInfo().getObjectIdentifier().getStringValue() : "NOT");
				map.put("H2_CLASS", cl.getH2_CLASS()); // 클래스
				map.put("H2_ATNAM", cl.getH2_ATNAM()); // 특성
				map.put("H2_ZGROUP", cl.getH2_ZGROUP()); // 특성그룹
				map.put("H2_ATBEZ", cl.getH2_ATBEZ()); // 특성명
				map.put("H2_ATFOR", cl.getH2_ATFOR()); // 데이터유형
				map.put("H2_ANZST", cl.getH2_ANZST()); // 길이
				map.put("H2_ANZDZ", cl.getH2_ANZDZ());// 소수자리
				map.put("H2_IPTYPE", cl.getH2_IPTYPE());// 입력방식
				map.put("H2_ATKLE", cl.getH2_ATKLE()); // 대소문자구분
				map.put("H2_ATAME", cl.getH2_ATAME()); // 다중값 허용
				map.put("H2_ATINT", cl.getH2_ATINT()); // 범위값 허용
				map.put("H2_CD_CLASS", cl.getH2_CD_CLASS()); // 특성값 공통코드ID
				map.put("H2_FIELDPROP", cl.getH2_FIELDPROP()); // 필수속성
				map.put("H2_PREFIX", cl.getH2_PREFIX()); // 자재내역 prifix
				map.put("H2_SUFFIX", cl.getH2_SUFFIX()); // 자재내역 suffix
				map.put("H2_ZORDER", cl.getH2_ZORDER()); // 정렬순서
				map.put("H2_DESC_FLAG", cl.getH2_DESC_FLAG()); // 자재내역 조합 대상
				map.put("H2_DESC_ORDER", cl.getH2_DESC_ORDER()); // 자재내역 조합 순서
				map.put("H2_REMARK", cl.getH2_REMARK()); // 한글명
				clWhere.add(map);
			}

			return clWhere;
		} catch (Exception e) {

			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();

			throw e;
		}

	}

	public String getStringValues(boolean isPart, ObjectClassificationMapping ocm, WTPart wp, WTPartMaster wpm)
			throws Exception {
		String response = "";

		try {
			QuerySpec query = new QuerySpec();
			String OCMOid = ocm.getPersistInfo().getObjectIdentifier().getStringValue();
			String OCMOidSplit = OCMOid.split(":")[1];

			String partOidSplit = null;
			String partMasterOidSplit = null;

			int idx = query.appendClassList(isPart ? Stringvalue2.class : Stringvalue3.class, true);

			if (isPart) {
				String partOid = wp.getPersistInfo().getObjectIdentifier().getStringValue();
				partOidSplit = partOid.split(":")[1];

				query.appendWhere(new SearchCondition(Stringvalue2.class, Stringvalue2.NH__WTPART__OID,
						SearchCondition.EQUAL, partOidSplit), idx);
				query.appendAnd();
				query.appendWhere(new SearchCondition(Stringvalue2.class, Stringvalue2.NH__CLASSIFICATION__OID,
						SearchCondition.EQUAL, OCMOidSplit), idx);
			} else {
				String partMasterOid = wpm.getPersistInfo().getObjectIdentifier().getStringValue();
				partMasterOidSplit = partMasterOid.split(":")[1];

				query.appendWhere(new SearchCondition(Stringvalue3.class, Stringvalue3.NH__WTPARTMASTER__OID,
						SearchCondition.EQUAL, partMasterOidSplit), idx);
				query.appendAnd();
				query.appendWhere(new SearchCondition(Stringvalue3.class, Stringvalue3.NH__CLASSIFICATION__OID,
						SearchCondition.EQUAL, OCMOidSplit), idx);
			}
			System.out.println("getStringValues::: " + query.toString());
			QueryResult results = PersistenceHelper.manager.find(query);
			while (results.hasMoreElements()) {
				Object[] obj = (Object[]) results.nextElement();
				if (isPart) {
					Stringvalue2 sv2 = (Stringvalue2) obj[0];
					response = sv2.getNH_VALUE();
				} else {
					Stringvalue3 sv3 = (Stringvalue3) obj[0];
					response = sv3.getNH_VALUE();
				}
			}

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

		return response;
	}

	// ---------------------------------------------------------------------------
	// [채번 대상 목록] 확인
	//
	//
	public String getApprovalNoBySerialListOid(String oid) throws Exception {
		String items = "";
		try {

			ReferenceFactory rf = new ReferenceFactory();
			Persistable persistable = null;
			SerialList sl = null;

			if (!oid.isEmpty() || !oid.isBlank()) {
				persistable = rf.getReference(oid).getObject();
				if (persistable instanceof SerialList) {
					sl = (SerialList) persistable;
					persistable = null;
					items = sl.getH2_ITEM_REQ_ID();
				} else {
					// throw new Exception("선택된 자재 찾기 불가로 수정시 새로운 자재가 생성 됩니다.");
					items = "REQ_" + generateSerialNumber();
				}
			} else {
				// throw new Exception("선택된 승인요청번호가 없습니다.");
				items = "REQ_" + generateSerialNumber();
			}

		} catch (Exception e) {

			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();

			throw e;
		}

		return items;
	}

	// ---------------------------------------------------------------------------
	// [자재 리스트] 속성 드롭다운 데이터 가져오기
	//
	public Map<String, Object> getDropType(String getOid) throws Exception {
		// Mapping Oid로 속성 데이터 찾기
		try {

		} catch (Exception e) {

		}

		return null;
	}

	// ------------------------------------------------------------------------------------------------
	// [채번요청] 결재요청 데이터 get
	//
	public Map<String, Object> getApprovalData(String serialListOid) throws Exception {
		// 체크 모드
		return getApprovalData(serialListOid, true);
	}

	public Map<String, Object> getApprovalData(String serialListOid, boolean isCheck) throws Exception {
		Map<String, Object> output = new HashMap<>();
		// Map<String, Object> outputData = new HashMap<>();
		List<Map<String, Object>> outputListData = new ArrayList<>();
		WTPrincipal prin = SessionHelper.manager.getPrincipal();
		Date now = new Date();
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			if (serialListOid == null) {
				throw new Exception("message: 결재할 요청번호가 존재하지 않습니다.");
			}

			ReferenceFactory rf = new ReferenceFactory();
			Persistable persistable = null;
			SerialList sl = null;

			persistable = rf.getReference(serialListOid).getObject();
			if (persistable instanceof SerialList) {
				sl = (SerialList) persistable;
				persistable = null;
			}

			// Header 불러오기
			output.put("Number", sl.getH2_ITEM_REQ_ID());
			output.put("TITLE", sl.getH2_SUBJECT());
			output.put("Body", sl.getH2_CONTENT());
			output.put("serialListOid", sl.getPersistInfo().getObjectIdentifier().getStringValue());

			// List Approval Data 불러오기
			QuerySpec query = new QuerySpec();

			int idx = query.appendClassList(SerialListMapping.class, true);
			int idx1 = query.appendClassList(WTPartSerialList.class, true);
			int idx2 = query.appendClassList(WTPart.class, true);

			// 조건을 작성할 때, String 값과 객체 식별자가 올바르게 매핑되었는지 확인
			SearchCondition condition17 = new SearchCondition(SerialListMapping.class,
					SerialListMapping.H2__WTPARTSERIALLIST__OID, WTPartSerialList.class,
					"thePersistInfo.theObjectIdentifier.id");
			condition17.setOuterJoin(SearchCondition.NO_OUTER_JOIN);
			query.appendWhere(condition17, new int[] { idx, idx1 });

			// 요청 번호에 대한 조건 추가
			query.appendAnd();
			query.appendWhere(new SearchCondition(SerialListMapping.class, SerialListMapping.H2__SERIALLIST__OID,
					SearchCondition.EQUAL, String.valueOf(serialListOid.split(":")[1])), new int[] { idx });

			query.appendAnd();
			SearchCondition condition18 = new SearchCondition(WTPartSerialList.class,
					WTPartSerialList.H2__WTPART__MST__OID, WTPart.class, "thePersistInfo.theObjectIdentifier.id");
			condition18.setOuterJoin(SearchCondition.NO_OUTER_JOIN);
			query.appendWhere(condition18, new int[] { idx1, idx2 });

			System.out.println(query.toString());

			// 쿼리 실행 및 결과 확인
			QueryResult results = PersistenceHelper.manager.find(query);

			// 결과 처리
			while (results.hasMoreElements()) {
				Object result[] = (Object[]) results.nextElement();
				SerialListMapping slm = (SerialListMapping) result[0];
				WTPartSerialList wsl = (WTPartSerialList) result[1];
				WTPart wp = (WTPart) result[2];
				EPMDocument epm = null;
				epm = getEPMDocumentByWTPart(wp);

				Map<String, Object> resultMap = new HashMap<>();
				resultMap.put("partOID", wp.getPersistInfo().getObjectIdentifier().getStringValue());
				resultMap.put("Class", wp.getTypeInfoWTPart().getPtc_str_1());
				resultMap.put("name", wp.getName());
				resultMap.put("number", wp.getNumber());

				if (epm != null) {
					resultMap.put("FileName", epm.getCADName());
					resultMap.put("epmDocumentOID", epm.getPersistInfo().getObjectIdentifier().getStringValue());
				}
				resultMap.put("partSerialListOID", wsl.getPersistInfo().getObjectIdentifier().getStringValue());
				resultMap.put("serialListOID", sl.getPersistInfo().getObjectIdentifier().getStringValue());
				resultMap.put("serialListMappingOID", slm.getPersistInfo().getObjectIdentifier().getStringValue());

				resultMap.put("ZCUE", "C");
				// 체크모드가 true 검즘
				if (isCheck) {
					resultMap.put("CHECK_REQ", "X");
				}

				outputListData.add(resultMap);
			}
			output.put("user", prin.getName());
			output.put("date", sdf1.format(now));
			output.put("list", outputListData);
		} catch (Exception e) {
			e.printStackTrace(); // 예외 발생 시 스택 트레이스를 출력
		}

		return output;
	}

//	public Map<String,Object>  getSerialAttribute(String Class) throws Exception {
//	
//    try {
//    	
//    	
//    	return null;
//    } catch (Exception e) {
//    	
//    	System.out.println("Error: " + e.getMessage());
//        e.printStackTrace();
//        
//        throw e;
//    }
//
//}	

	// ---------------------------------------------------------------------------
	// [임의] 임의 시리얼 넘버
	//
	//
	public String generateSerialNumber() {
		//
		String date = java.time.LocalDate.now().toString().replace("-", "");
		long ticks = System.currentTimeMillis();
		String tickString = String.valueOf(ticks);
		String tickLast3Digits = tickString.length() > 3 ? tickString.substring(tickString.length() - 3) : tickString;
		String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 2);
		long randomValue = (long) (Math.random() * Math.pow(2, 48));
		String random48bit = String.format("%012x", randomValue);
		String random2Digits = random48bit.substring(0, 2);

		return date + tickLast3Digits + uuid + random2Digits;
	}

	// ---------------------------------------------------------------------------
	// [임의] 인터페이스에서 호출 분기 승인결재, 미결재건
	//
	public <T> String getSerialApprovalQuarter(Map<String, Object> data) throws Exception {
		try {
			Map<String, Object> input = data;

			boolean isApproval = (boolean) input.get("isApproval");
			// boolean isCheck = (boolean) input.get("isCheck");
			String cshtw = Objects.toString(input.get("cshtw"), ""); // 부품 상세 - 특수 목적
			List<Map<String, Object>> actionData = (List<Map<String, Object>>) input.get("List");
			System.out.println("input " + input);
			// TODO: .... 부품이 정상적인지 판단 하기. 부품에 대한 정보 확인하기.
			System.out.println("input.get(\"List\") " + input.get("List"));
			if (isApproval) { // true인 경우 결재건
				Map<String, Object> result = (Map<String, Object>) RestAPIHelper.manager
						.IFSMD0007((List<Map<String, Object>>) input.get("List"));

				throw new Exception("Message: MDIF001(Line:979) 점검 테스트");
				// return actionSerialApproval((ArrayList<Map<String,Object>>)
				// input.get("List"), null);

			} else { // false 인경우 미결재건

				// I/F 호출
				Map<String, Object> result = (Map<String, Object>) RestAPIHelper.manager.IFSMD0007(actionData);

				if (result == null) {
					throw new Exception("Message: I/F IFSMD0007 Call Error.. Line:971");
				}

				// PICODE... PIDATE... PITIME... PISTAT... PIMSG...
				Map<String, Object> ifstat = (Map<String, Object>) result.get("IFSTAT");
				for (Map<String, Object> item : actionData) {
					item.remove("CHECK_REQ");
				}

				// 결과 Return
				Object et_result = result.get("ET_RESULT");
				Object et_dup_matnr = result.get("ET_DUP_MATNR");
				System.out.println("CHECK_REQ X:::: " + et_dup_matnr);
				if (et_result instanceof Map etMap) {
					Map<String, Object> datas = (Map<String, Object>) etMap;
					String RESULT = (String) datas.get("RESULT");
					String MESSAGE = Objects.toString(data.get("MESSAGE"), "MD Interface Error: MD → PLM 채번 검증 실패");
					// messageChange(params.. ,datas)

					// I/F시 오류 발생된 경우
					if (RESULT.equals("E")) {
						throw new Exception("Message: " + MESSAGE);
					}
					result = (Map<String, Object>) RestAPIHelper.manager.IFSMD0007(actionData);
					ifstat = (Map<String, Object>) result.get("IFSTAT");
					et_dup_matnr = result.get("ET_DUP_MATNR");
					et_result = result.get("ET_RESULT");
					et_dup_matnr = result.get("ET_DUP_MATNR");
					System.out.println("CHECK_REQ OK:::: " + et_dup_matnr);
				} else if (et_result instanceof List<?> etList) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>) etList;
					for (Map<String, Object> data1 : datas) {
						String RESULT = (String) data1.get("RESULT");
						String MESSAGE = Objects.toString(data1.get("MESSAGE"),
								"MD Interface Error: MD → PLM 채번 검증 실패");

						// I/F시 오류 발생된 경우
						if (RESULT.equals("E")) {
							throw new Exception("Message: " + MESSAGE);
						}
					}
					result = (Map<String, Object>) RestAPIHelper.manager.IFSMD0007(actionData);
					ifstat = (Map<String, Object>) result.get("IFSTAT");
					et_result = result.get("ET_RESULT");
					et_dup_matnr = result.get("ET_DUP_MATNR");
					System.out.println("CHECK_REQ OK:::: " + et_dup_matnr);
				}

				if (et_result instanceof Map etMap) {
					Map<String, Object> datas = (Map<String, Object>) etMap;
					String RESULT = (String) datas.get("RESULT");
					String MESSAGE = Objects.toString(data.get("MESSAGE"), "MD Interface Error: MD → PLM 채번 검증 실패");
					// I/F시 오류 발생된 경우
					if (RESULT.equals("E")) {
						throw new Exception("Message: " + MESSAGE);
					}
				} else if (et_result instanceof List<?> etList) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>) etList;
					for (Map<String, Object> data1 : datas) {
						String RESULT = (String) data1.get("RESULT");
						String MESSAGE = Objects.toString(data1.get("MESSAGE"),
								"MD Interface Error: MD → PLM 채번 검증 실패");

						// I/F시 오류 발생된 경우
						if (RESULT.equals("E")) {
							throw new Exception("Message: " + MESSAGE);
						}
					}
				}

				System.out.println("state: " + ifstat);
				System.out.println("result: " + et_result);

				// cshtw "X"아닌 경우
				if (!cshtw.equals("X")) {
					return actionSerialApproval(actionData, et_result);
				} else {
					// throw new Exception("Message: MDIF001(Line:989) 특수채번 점검 테스트");
					return actionSpecialSerial(actionData, (Map<String, Object>) et_result);
				}
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			throw e;
		}
	}

	// ---------------------------------------------------------------------------
	// [임의] 인터페이스에서 호출 (wind => rest 에서 call (md i/f json 데이터 만듬)
	//
	public <T> Map<String, Object> getMDMSerialInterface(T data) throws Exception {

		try {
			Map<String, Object> json = new HashMap<>();
			System.out.println("-------------------------------");
			List<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>) data;
			Map<String, String> matCOM = new HashMap<>(); //
			List<Map<String, String>> matGEN = new ArrayList<>(); //
			List<Map<String, String>> matPLANT = new ArrayList<>(); //
			List<Map<String, String>> matCLASSIF = new ArrayList<>(); //
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");

			for (Map<String, Object> output : outputs) {
				// getSerialEdit
				Map<String, Object> editMap = new HashMap<>();
				Map<String, Object> editMapAction = new HashMap<>();

				String outClass = Objects.toString(output.get("Class"), ""); // Class 분류체계
				String partOID = Objects.toString(output.get("partOID"), ""); // partOID 자재oid

				WTPart wp = (WTPart) getClasszz(partOID);
				WTPartMaster wpm = null;
				if (wp != null) {
					wpm = wp.getMaster();
				}
				String wpmOid = String.valueOf(wpm.getPersistInfo().getObjectIdentifier().getId());
				String wpmNumber = wpm.getNumber();
				String epmDocumentOID = Objects.toString(output.get("epmDocumentOID"), ""); // epmDocumentOID 문서 oid
				String ZCUE = Objects.toString(output.get("ZCUE"), ""); // 생성 채번 : C , 설계 변경 : U , 플랜트 확장 : E
				String CHECK_REQ = Objects.toString(output.get("CHECK_REQ"), ""); // 점검 요청: X - 점검 요청 필요 없는 경우 null
				String PRDHA = Objects.toString(output.get("PRDHA"), ""); // 제품계층 구조
				String ZZSPTYPE = Objects.toString(output.get("ZZSPTYPE"), "C"); // 목적유형
				// String ZZORG_MATNR = Objects.toString(output.get("ZZORG_MATNR"), "");// 참조
				// 자재번호
				String ZZCHGNO = Objects.toString(output.get("ZZCHGNO"), ""); // 변경번호 (S,P,H등 하기 전 기존 번호)
				String ZZCHGNO_DESC = Objects.toString(output.get("ZZCHGNO_DESC"), "");// 변경요청 내역
				String partSerialListOID = Objects.toString(output.get("partSerialListOID"), ""); // partSerialListOID
																									// 채번리스트 oid
				String serialListOID = Objects.toString(output.get("serialListOID"), ""); // serialListOID 승인요청 oid
				String serialListMappingOID = Objects.toString(output.get("serialListMappingOID"), ""); // serialListMappingOID
																										// 채번+승인요청 매핑
																										// oid

				editMapAction.put("partOID", partOID);
				editMapAction.put("Class", outClass);
				editMapAction.put("partSerialListOID", partSerialListOID);
				editMapAction.put("epmDocumentOID", epmDocumentOID);
				editMapAction.put("serialListOID", serialListOID);
				editMapAction.put("serialListMappingOID", serialListMappingOID);
				editMap.put("1", editMapAction);

				Map<String, Object> attributeEdit = getWTPartToEdit(editMap);
				System.out.println("AttributeType: " + attributeEdit);

				// attributeEdit objectOid, class 제거 하드코딩...
				attributeEdit.remove("partSerialListOID");
				attributeEdit.remove("serialListOID");
				attributeEdit.remove("partOID");
				attributeEdit.remove("Class");
				attributeEdit.remove("serialListMappingOID");
				attributeEdit.remove("epmDocumentOID");

				if (CHECK_REQ != "") {
					matCOM.put("CHECK_REQ", CHECK_REQ); // 채번에 점검 요청: X - 처음 채번전 점검 요청필요
				} else {
					matCOM.put("CHECK_REQ", ""); // 채번에 점검 요청: X - 처음 채번전 점검 요청필요
				}
				matCOM.put("CHECK_REQ", "X");
				System.out.println("wpm.getDefaultUnit()" + wpm.getDefaultUnit());

				// Map.entry("MTART", ""), // 자재 유형
				// Map.entry("CLASS", outClass), // 분류체계
				// Map.entry("MATKL", ""), // 자재그룹 // PLM 관리 안함
				// Map.entry("SPART", ""), // 제품군 // PLM 관리 안함
				// Map.entry("GEWEI", ""), // 중량단위
				// Map.entry("MEABM", ""), // 치수단위
				// Map.entry("ZZCBM_BREIT", ""), // CBM 가로
				// Map.entry("ZZCBM_LAENG", ""), // CBM 세로
				// Map.entry("ZZCBM_HOEHE", ""), // CBM 높이
				// Map.entry("ZZCBM_WEIGHT", ""), // CBM 무게
				// Map.entry("MBRSH", ""),
				// Map.entry("ZFORM", (String) attributeEdit.get("HR")), // HR 속성 정보 (형상기호)
				// Map.entry("ZZSP_YN", ""),
				// Map.entry("ZZSP_LV", ""),
				// Map.entry("ZZMDLNM", ""),
				// Map.entry("ZZSPEC", ""),
				// Map.entry("ZZMJREV", ""),
				// Map.entry("ZZCHANGENO", ""),

				String ZZORG_MATNR = "";
				if (ZZSPTYPE != "C") {
					ZZORG_MATNR = wpm.getNumber();
				}
				String ZZDESC = wpm.getName();
				String MAKTX = "";
				if (ZZDESC.length() > 40) {
					MAKTX = ZZDESC.substring(0, 40);
				} else {
					MAKTX = ZZDESC;
				}

				matGEN.add(Map.ofEntries(Map.entry("MATNR", ""), // 자재 품번 wpm.getNumber()
						Map.entry("ZZPLM_ID", wpmOid), // 자재 마스터 oid
						Map.entry("ZCUE", ZCUE), // 생성 채번 : C , 설계 변경 : U , 플랜트 확장 : E
						Map.entry("MEINS", wpm.getDefaultUnit().toString()), // 기본단위
						Map.entry("PRDHA", PRDHA), // 하이라이키( 부품일땐 안줘도됨, 하지만 제품일땐 무조건 줘야함 )
						Map.entry("ZZSPTYPE", ZZSPTYPE), // 디폴트 C , 이외 특수목적 S, W, 등등

						// C가 아닌경우 원품번을 보내달라 - 특수목적(S일때 이전 자재번호)C 일반 클래스가 있다
						// (분류체계, 분류의 특성값 나머진 없다) , S 보수품, H 법인간, P MTS 판매, W Withowt bom,
						// U Up-Kit | ( PLM에서 부터 온다 )
						Map.entry("ZZORG_MATNR", ZZORG_MATNR), // 참조 자재번호
						Map.entry("MAKTX", MAKTX), // 자재내역 Key-In (아래 상세내역 보내달라 40자로 짤라서)
						Map.entry("ZZDESC", ZZDESC), // 자재상세내역 H5X-L-F(ClassHeader : H2_PREFIX_DESC + ClassItem에 Order
														// 포함) + 자동생성
						Map.entry("ZZULMD", ""), // UL 모델명
						Map.entry("ZZROH", ""), // ROHS관리유무
						Map.entry("ZESTCOST", ""), // 추정원가
						Map.entry("ZZQC_TEST", ""), // 품질검사 대상(개발품)
						Map.entry("ZZQC_RESULT", ""), // 품질검사 결과(개발품)
						Map.entry("ZZCHGNO", ZZCHGNO), // 변경번호 (S,P,H등 하기 전 기존 번호)
						Map.entry("ZZCHGNO_DESC", ZZCHGNO_DESC), // 변경요청 내역
						Map.entry("ZZHWREV", wp.getVersionIdentifier().getValue()), // Revision
						Map.entry("ZZERDAT", sdf1.format(wp.getCreateTimestamp())), // 생성일
						Map.entry("ZZERNAM", wp.getCreatorName()), // 생성자 id
						Map.entry("ZZAEDAT", sdf1.format(wp.getModifyTimestamp())), // 변경일
						Map.entry("ZZAENAM", wp.getModifierName()) // 변경자 id
				));

				matPLANT.add(Map.ofEntries(Map.entry("MATNR", ""), // 자재 품번
						Map.entry("ZZPLM_ID", wpmOid), // wpmOid 자재 마스터 oid
						Map.entry("WERKS", "1100"), // Objects.toString(IBAUtils.getStringValue(wp, "PLANT"),"") 플랜트
						Map.entry("ZZTYPECD", "AA"), // 타입코드 TEST
						Map.entry("BESKZ", "F"), // 조달유형 TEST
						Map.entry("SOBSL", ""), // 특별조달유형
						Map.entry("ZZSERI", "N"), // Serial 관리유무 TEST
						Map.entry("DISMM", ""), // MRP 유형
						Map.entry("KZKRI", "") // Final Mecha Ind.
				));

				System.out.println(wpm.getNumber());
				attributeEdit.forEach((key, value) -> {
					System.out.println(key);
					// System.out.println(outClass);
					String valueTo = "";
					String atnam = "";
					String[] getData = getFindAttributeTypeData(key, outClass);
					// 확인 할 것 H2_ATNAM(CH2195) , H2_ATINT 범위값
					// H2_ATINT 범위값 허용
					System.out.println(getData);

					if (getData.length > 0) {
						atnam = getData[0];
						if (getData[1] != null) {
							String[] out = value.toString().split("|");
							value = out[0];
							valueTo = out[1];
						}
					}
					if (atnam != "") {
						matCLASSIF.add(Map.ofEntries(Map.entry("MATNR", ""), // 자재 품번 wpm.getNumber()
								Map.entry("ZZPLM_ID", wpmOid), // 자재 마스터 oid(ida2a2)
								Map.entry("CLASS", outClass), // 분류체계
								Map.entry("CHARACTER", atnam), // 특성id (H2_ATNAM)
								// H2_ATINT X 인경우 value 분리해서 각각 넣기
								Map.entry("VALUE_FROM", Objects.toString(value, "")), // 특성값 (From) 복수의 값인경우 '|'로 구분 하고
																						// VALUE_FROM ,
								Map.entry("VALUE_TO", valueTo) // 특성값 (To) from ~ to (plm |로 되어있는것을 인경우는 VALUE_FROM /
																// VALUE_TO 에 각각 넣어줌
						));
					}
				});

			}
			json.put("MAT_COM", matCOM);
			json.put("MAT_GEN", matGEN);
			json.put("MAT_PLANT", matPLANT);
			json.put("MAT_CLASSIF", matCLASSIF);

			// System.out.println("JSON: " +
			// com.hyosung.tnsplm.controllers.APIController.convertMapToJson(json));
			return json;
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

	}
	

	
	

	private Map<String, Object> processMap(Map<?, ?> map) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();

			if (value instanceof Map) {
				result.put(key, processMap((Map<?, ?>) value));
			} else if (value instanceof Iterable) {
				result.put(key, value);
			} else {
				result.put(key, value);
			}
		}
		return result;
	}

	public static Object getClasszz(String oid) throws Exception {
		try {
			ReferenceFactory rf = new ReferenceFactory();
			Persistable persistable = null;
			if (!oid.isEmpty() || !oid.isBlank()) {
				persistable = rf.getReference(oid).getObject();

				if (persistable instanceof WTPart) {
					WTPart wp = (WTPart) persistable;
					persistable = null;
					return wp;
				} else if (persistable instanceof EPMDocument) {
					EPMDocument epm = (EPMDocument) persistable;
					persistable = null;
					return epm;
				} else if (persistable instanceof WTPartSerialList) {
					WTPartSerialList wpsl = (WTPartSerialList) persistable;
					persistable = null;
					return wpsl;
				} else if (persistable instanceof SerialList) {
					SerialList sl = (SerialList) persistable;
					persistable = null;
					return sl;
				} else if (persistable instanceof SerialListMapping) {
					SerialListMapping slm = (SerialListMapping) persistable;
					persistable = null;
					return slm;
				}

//		        	else {
//		        		throw new Exception("선택된 자재 찾기 불가로 수정시 새로운 자재가 생성 됩니다.");
//		        	}
			}
			return null;
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

	}

	// Check and sink the latest version
	public static void checkSinkLatestVersion(WTPart part) throws Exception {
		Transaction tnx = null;
		try {
			tnx = new Transaction();
			tnx.start();

			tnx.commit();
			tnx = null;
		} catch (Exception e) {
			if (tnx != null) {
				tnx.rollback();
			}
			e.printStackTrace();
			throw e;
		}
	}

//	@Autowired
//	private serialDao dao;
//	
//	public List<Map<String,Object>> selectClassForType(Map<String, Object> params) throws Exception{
//		List<Map<String,Object>> rtnData = new ArrayList<Map<String,Object>> ();
//		SqlSession sqlSession = tnsSessionManager.getSqlSession();
//		rtnData = dao.selectClassForType(sqlSession, params);
//		return rtnData;
//	}
	/**
	 * H2_PLM_PROP으로 ObjClassificationMapping oid 가져오기
	 * 
	 * @param value
	 * @return
	 * @throws Exception
	 */
	public ObjectClassificationMapping getObjClassMapping(String clazz, String value) throws Exception {
		ObjectClassificationMapping obj = null;

		QuerySpec query = new QuerySpec();
		int idx = query.appendClassList(ObjectClassificationMapping.class, true);
		int idxClassItem = query.appendClassList(ClassItem.class, true);

		SearchCondition sc = new SearchCondition();
		
		sc = new SearchCondition(ClassItem.class, ClassItem.H2__CLASS, "=",
				clazz);
		query.appendWhere(sc, new int[] { idxClassItem });
		
		query.appendAnd();
		
		sc = new SearchCondition(ClassItem.class, ClassItem.H2__ATNAM,
				ObjectClassificationMapping.class, ObjectClassificationMapping.H2__ATNAM
				);
		query.appendWhere(sc, new int[] { idxClassItem, idx });
		
		query.appendAnd();

		sc = new SearchCondition(ObjectClassificationMapping.class, ObjectClassificationMapping.H2__PLM__PROP, "=",
				value);
		query.appendWhere(sc, new int[] { idx });
		System.out.println("" + query.toString());
		QueryResult qr = PersistenceHelper.manager.find(query);

		if (qr.hasMoreElements()) {
			Object[] oo = (Object[]) qr.nextElement();
			obj = (ObjectClassificationMapping) oo[0];
		}
		return obj;
	}

	public List<Map<String, Object>> getFindObjectClassificationMapping(String checkerClass) throws Exception {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		try {
			QuerySpec query = new QuerySpec();
			int idxCH = query.appendClassList(ClassHeader.class, false);
			int idxCI = query.appendClassList(ClassItem.class, false);
			int idxOCM = query.appendClassList(ObjectClassificationMapping.class, true);

			query.appendWhere(
					new SearchCondition(ClassHeader.class, ClassHeader.H2__CLASS, SearchCondition.EQUAL, checkerClass));

			query.appendAnd();

			query.appendWhere(
					new SearchCondition(ClassHeader.class, ClassHeader.H2__CLASS, ClassItem.class, ClassItem.H2__CLASS),
					idxCH, idxCI);

			query.appendAnd();

			query.appendWhere(new SearchCondition(ClassItem.class, ClassItem.H2__ATNAM,
					ObjectClassificationMapping.class, ObjectClassificationMapping.H2__ATNAM), idxCI, idxOCM);

			QueryResult results = PersistenceHelper.manager.find(query);
			while (results.hasMoreElements()) {
				Object[] obj = (Object[]) results.nextElement();
				ObjectClassificationMapping ocm = (ObjectClassificationMapping) obj[0];
				Map<String, Object> dataMap = new HashMap<String, Object>();
				// H2_ATNAM, H2_PLM_PROP, H2_PROP_TYPE
				dataMap.put("oid", ocm.getPersistInfo().getObjectIdentifier().getId());
				dataMap.put("H2_ATNAM", ocm.getH2_ATNAM());
				dataMap.put("H2_PLM_PROP", ocm.getH2_PLM_PROP());
				dataMap.put("H2_PROP_TYPE", ocm.getH2_PROP_TYPE());
				resultList.add(dataMap);
			}

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return resultList;
	}

	// StringValue2 , StringValue3 | oid
	public int getFindCheckerStringValue(String target, String checkerClass) throws Exception {
		try {
			QuerySpec query = new QuerySpec();
			Class<?> targetClass;
			QueryResult queryResult;

			switch (target) {
			case "StringValue2":
				int idxSV2 = query.appendClassList(Stringvalue2.class, true);
				targetClass = Stringvalue2.class;
				query.appendWhere(new SearchCondition(targetClass, Stringvalue2.NH__CLASSIFICATION__OID,
						SearchCondition.EQUAL, checkerClass), idxSV2);
				System.out.println("Executing Query for StringValue2: " + query);
				queryResult = PersistenceHelper.manager.find(query);
				return queryResult.size();

			case "StringValue3":
				int idxSV3 = query.appendClassList(Stringvalue3.class, true);
				targetClass = Stringvalue3.class;
				query.appendWhere(new SearchCondition(targetClass, Stringvalue3.NH__CLASSIFICATION__OID,
						SearchCondition.EQUAL, checkerClass), idxSV3);
				System.out.println("Executing Query for StringValue3: " + query);
				queryResult = PersistenceHelper.manager.find(query);
				return queryResult.size();

			default:
				throw new IllegalArgumentException("Invalid target: " + target);
			}
		} catch (Exception e) {
			System.err.println("Error executing query: " + e.getMessage());
			throw new Exception("Error in getFindCheckerStringValue: " + e.getMessage(), e);
		}
	}

	public void epmListWhere(QuerySpec query, Map<String, Object> params, int idxPart, int idxPartMaster,
			int idxBuildRule, int idxDoc, int idxDocMaster, int idxUser) throws Exception {
		try {
			String folderOid = Objects.toString(params.get("folderOid"), ""); // contextName: 조회 폴더
			String contextName = Objects.toString(params.get("contextName"), ""); // contextName: 조회 폴더
			String cadName = Objects.toString(params.get("cadName"), ""); // Name: "파일명"
			String number = Objects.toString(params.get("Number"), "");// Number: "품번"
			String status = Objects.toString(params.get("Status"), "");// Status: "상태"
			String var = Objects.toString(params.get("Var"), "newVar");// Var : "newVer" 최신 or "allVer" 모든
			String regUser = Objects.toString(params.get("RegUser"), "");// RegUser: "작성자"
			String modUser = Objects.toString(params.get("ModUser"), "");// ModUser: "수정자"
			String regStartDate = Objects.toString(params.get("RegStartDate"), "");// RegStartDate: "2025-04-15" 작성일자 시작
			String modStartDate = Objects.toString(params.get("ModStartDate"), "");// ModStartDate: "2025-04-15" 수정일자 시작
			String regEndDate = Objects.toString(params.get("RegEndDate"), "");// RegEndDate: "2025-04-16" 작성일자 종료
			String modEndDate = Objects.toString(params.get("ModEndDate"), "");// ModEndDate: "2025-04-16" 수정일자 종료
			String project = Objects.toString(params.get("project"), ""); // 프로젝트 여부
			SearchCondition sc = null;

			if (!folderOid.isEmpty() && !contextName.isEmpty()) {
				// 폴더 OID에서 실제 폴더 ID 추출 (예: "291393")
				String folderId = folderOid.split(":")[1];

//				String[] pathParts = contextName.split("/");
//				String subFolderPath = "/" + String.join("/", java.util.Arrays.copyOfRange(pathParts, 2, pathParts.length));
//				String context = pathParts[1];
//				WTContainerRef container = CommonUtils.getContainer(context);

				// EPMDocument와 폴더를 연결하는 조건을 추가
				sc = new SearchCondition(EPMDocument.class, "folderingInfo.parentFolder.key.id", // 폴더 OID와 연결된 필드 (실제
																									// 필드 이름은 시스템에 맞게 수정
																									// 필요)
						SearchCondition.IN, Long.valueOf(folderId) // 폴더 ID만 사용
				);
				query.appendWhere(sc, new int[] { idxDoc });
				query.appendAnd();
			}

			if (!cadName.isEmpty()) {
				sc = new SearchCondition(EPMDocumentMaster.class, EPMDocumentMaster.CADNAME, SearchCondition.LIKE,
						"%" + cadName + "%", false);
				query.appendWhere(sc, new int[] { idxDocMaster });
				query.appendAnd();
			}

			if (!project.isEmpty()) {
				sc = new SearchCondition(EPMDocument.class, "containerReference.key.classname", SearchCondition.EQUAL,
						project);
				query.appendWhere(sc, new int[] { idxDoc });
				query.appendAnd();
			}

			if (!number.isEmpty()) {
				sc = new SearchCondition(EPMDocumentMaster.class, EPMDocumentMaster.NUMBER, SearchCondition.LIKE,
						"%" + number + "%");
				query.appendWhere(sc, new int[] { idxDocMaster });
				query.appendAnd();
			}

			if (!regUser.isEmpty()) {
				sc = new SearchCondition(WTUser.class, WTUser.FULL_NAME, SearchCondition.LIKE, "%" + regUser + "%");
				query.appendWhere(sc, new int[] { idxUser });
				query.appendAnd();
			}

			if (!modUser.isEmpty()) {
				sc = new SearchCondition(WTUser.class, WTUser.FULL_NAME, SearchCondition.LIKE, "%" + modUser + "%");
				query.appendWhere(sc, new int[] { idxUser });
				query.appendAnd();
			}

			if (!regStartDate.isEmpty() || !regEndDate.isEmpty()) {
				if (!regStartDate.equals("")) {
					SearchCondition startCondition = new SearchCondition(EPMDocument.class,
							EPMDocument.CREATE_TIMESTAMP, SearchCondition.GREATER_THAN_OR_EQUAL,
							Timestamp.valueOf(regStartDate + " 00:00:00"));
					query.appendWhere(startCondition, new int[] { idxDoc });
					query.appendAnd();
				}

				if (!regEndDate.equals("")) {
					SearchCondition endCondition = new SearchCondition(EPMDocument.class, EPMDocument.CREATE_TIMESTAMP,
							SearchCondition.LESS_THAN_OR_EQUAL, Timestamp.valueOf(regEndDate + " 23:59:59"));
					query.appendWhere(endCondition, new int[] { idxDoc });
					query.appendAnd();
				}
			}

			if (!modStartDate.isEmpty() || !modEndDate.isEmpty()) {
				if (!modStartDate.equals("")) {
					SearchCondition startCondition = new SearchCondition(EPMDocument.class,
							EPMDocument.MODIFY_TIMESTAMP, SearchCondition.GREATER_THAN_OR_EQUAL,
							Timestamp.valueOf(modStartDate + " 00:00:00"));
					query.appendWhere(startCondition, new int[] { idxDoc });
					query.appendAnd();
				}

				if (!modEndDate.equals("")) {
					SearchCondition endCondition = new SearchCondition(EPMDocument.class, EPMDocument.MODIFY_TIMESTAMP,
							SearchCondition.LESS_THAN_OR_EQUAL, Timestamp.valueOf(modEndDate + " 23:59:59"));
					query.appendWhere(endCondition, new int[] { idxDoc });
					query.appendAnd();
				}
			}

			if (var != null && (!var.isEmpty() || !var.isBlank())) {
				if (var.equals("newVar")) {
					query.appendWhere(
							new SearchCondition(WTPart.class, Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE),
							new int[] { idxPart });
					query.appendAnd();
					query.appendWhere(
							new SearchCondition(EPMDocument.class, Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE),
							new int[] { idxDoc });
				} else if (var.equals("allVar")) {
					query.appendWhere(
							new SearchCondition(WTPart.class, Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE),
							new int[] { idxPart });
					query.appendAnd();
					query.appendOpenParen();
					query.appendWhere(
							new SearchCondition(EPMDocument.class, Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE),
							new int[] { idxDoc });
					query.appendOr();
					query.appendWhere(
							new SearchCondition(EPMDocument.class, Iterated.LATEST_ITERATION, SearchCondition.IS_FALSE),
							new int[] { idxDoc });
					query.appendCloseParen();
				} else {
					throw new Exception("message: 잘못된 버전정보");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public String getFindClassHeaderIS3S(String Class) throws Exception {
		String data = "";

		if (Class == null || Class.isEmpty()) {
			return "";
		}

		try {
			QuerySpec query = new QuerySpec();
			int idx = query.appendClassList(ClassHeader.class, true);
			SearchCondition sc = new SearchCondition(ClassHeader.class, "H2_CLASS", SearchCondition.EQUAL, Class);
			query.appendWhere(sc, new int[] { idx });
			System.out.println("" + query.toString());
			QueryResult results = PersistenceHelper.manager.find(query);
			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ClassHeader cl = (ClassHeader) obj[0]; // ClassItem 데이터가 2번째 인덱스
				data = cl.getH2_IS_3S();
				return data;
			}

		} catch (Exception e) {
			return "";
			// throw new Exception("message: Find Class Header Error");
		}
		return data;
	}

	// H2_PLM_PROP(objectclassificationmapping), H2_CLASS(classitem)
	public String[] getFindAttributeTypeData(String prop, String Class) {
		try {
			String data[] = null;
			ArrayList<String> dataList = new ArrayList<>();

			QuerySpec query = new QuerySpec();
			int idx = query.appendClassList(ObjectClassificationMapping.class, false);
			int idx1 = query.appendClassList(ClassItem.class, true);

			SearchCondition sc = new SearchCondition(ObjectClassificationMapping.class, "H2_ATNAM", ClassItem.class,
					"H2_ATNAM");
			query.appendWhere(sc, new int[] { 0, 1 });
			query.appendAnd();

			// prop 조건 추가
			sc = new SearchCondition(ObjectClassificationMapping.class, "H2_PLM_PROP", SearchCondition.EQUAL, prop);
			query.appendWhere(sc, new int[] { 0 });
			query.appendAnd();

			// Class 조건 추가
			sc = new SearchCondition(ClassItem.class, "H2_CLASS", SearchCondition.EQUAL, Class);
			query.appendWhere(sc, new int[] { 1 });
			System.out.println("query " + query.toString());
			QueryResult results = PersistenceHelper.manager.find(query);
			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ClassItem cl = (ClassItem) obj[0]; // ClassItem 데이터가 2번째 인덱스
				dataList.add(cl.getH2_ATNAM());
				dataList.add(cl.getH2_ATINT());
			}
			data = new String[dataList.size()];
			dataList.toArray(data);
			System.out.println("getFindAttributeTypeData: " + java.util.Arrays.toString(data));
			return data;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------------
	// [채번요청] 승인 요청 데이터 객체 생성
	//
	public Map<String, Object> createApprovalRequestData(String OID, boolean CHECK_REQ) throws Exception {
		Map<String, Object> output = new HashMap<>();
		// Map<String, Object> outputData = new HashMap<>();
		List<Map<String, Object>> outputListData = new ArrayList<>();
		WTPrincipal prin = SessionHelper.manager.getPrincipal();
		Date now = new Date();

		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			if (OID == null) {
				throw new Exception("message: 결재할 요청번호가 존재하지 않습니다.");
			}

			ReferenceFactory rf = new ReferenceFactory();
			Persistable persistable = null;
			SerialList sl = null;

			persistable = rf.getReference(OID).getObject();
			if (persistable instanceof SerialList) {
				sl = (SerialList) persistable;
				persistable = null;
			}

			// Header 불러오기
			// output.put("Number", sl.getH2_ITEM_REQ_ID());
			// output.put("TITLE", sl.getH2_SUBJECT());
			// output.put("Body", sl.getH2_CONTENT());
			// output.put("serialListOid",
			// sl.getPersistInfo().getObjectIdentifier().getStringValue());

			// List Approval Data 불러오기
			QuerySpec query = new QuerySpec();

			int idx = query.appendClassList(SerialListMapping.class, true);
			int idx1 = query.appendClassList(WTPartSerialList.class, true);
			int idx2 = query.appendClassList(WTPart.class, true);

			// 조건을 작성할 때, String 값과 객체 식별자가 올바르게 매핑되었는지 확인
			SearchCondition condition17 = new SearchCondition(SerialListMapping.class,
					SerialListMapping.H2__WTPARTSERIALLIST__OID, WTPartSerialList.class,
					"thePersistInfo.theObjectIdentifier.id");
			condition17.setOuterJoin(SearchCondition.NO_OUTER_JOIN);
			query.appendWhere(condition17, new int[] { idx, idx1 });

			// 요청 번호에 대한 조건 추가
			query.appendAnd();
			query.appendWhere(new SearchCondition(SerialListMapping.class, SerialListMapping.H2__SERIALLIST__OID,
					SearchCondition.EQUAL, String.valueOf(OID.split(":")[1])), new int[] { idx });

			query.appendAnd();
			SearchCondition condition18 = new SearchCondition(WTPartSerialList.class,
					WTPartSerialList.H2__WTPART__MST__OID, WTPart.class, "thePersistInfo.theObjectIdentifier.id");
			condition18.setOuterJoin(SearchCondition.NO_OUTER_JOIN);
			query.appendWhere(condition18, new int[] { idx1, idx2 });

			System.out.println(query.toString());

			// 쿼리 실행 및 결과 확인
			QueryResult results = PersistenceHelper.manager.find(query);
			String joinOidList = "";
			// 결과 처리
			while (results.hasMoreElements()) {
//            	String PRDHA = Objects.toString(output.get("PRDHA"), "");  // 제품계층 구조		?
//            	String ZZSPTYPE = Objects.toString(output.get("ZZSPTYPE"), "");  // 목적유형		?
//            	String ZZORG_MATNR = Objects.toString(output.get("ZZORG_MATNR"), "");// 참조 자재번호	?
//            	String ZZCHGNO = Objects.toString(output.get("ZZCHGNO"), "");   // 변경번호 (S,P,H등 하기 전 기존 번호)	?
//            	String ZZCHGNO_DESC = Objects.toString(output.get("ZZCHGNO_DESC"), "");// 변경요청 내역	?

				Object result[] = (Object[]) results.nextElement();
				SerialListMapping slm = (SerialListMapping) result[0];
				WTPartSerialList wsl = (WTPartSerialList) result[1];
				WTPart wp = (WTPart) result[2];
				EPMDocument epm = null;
				epm = getEPMDocumentByWTPart(wp);

				Map<String, Object> resultMap = new HashMap<>();
				resultMap.put("partOID", wp.getPersistInfo().getObjectIdentifier().getStringValue());
				resultMap.put("Class", wp.getTypeInfoWTPart().getPtc_str_1());

				if (epm != null)
					resultMap.put("epmDocumentOID", epm.getPersistInfo().getObjectIdentifier().getStringValue());

				if (joinOidList.isEmpty()) {
					joinOidList = wp.getPersistInfo().getObjectIdentifier().getStringValue();
				} else {
					joinOidList += "|" + wp.getPersistInfo().getObjectIdentifier().getStringValue();
				}

				resultMap.put("ZCUE", "C");
				if (CHECK_REQ) { // 점검 요청
					resultMap.put("CHECK_REQ", "X");
				} else {
					resultMap.put("CHECK_REQ", "");
				}

				outputListData.add(resultMap);
			}
			// output.put("user", prin.getName());
			// output.put("date", sdf1.format(now));
			output.put("oid", joinOidList);
			output.put("list", outputListData);
		} catch (Exception e) {
			e.printStackTrace(); // 예외 발생 시 스택 트레이스를 출력
		}

		return output;
	}

	public String getClassHeader(String Class) throws Exception {
		String result = "";
		QuerySpec query = new QuerySpec();
		int idx = query.appendClassList(ClassHeader.class, true);

		SearchCondition sc = new SearchCondition(ClassHeader.class, "H2_CLASS", SearchCondition.EQUAL, Class);
		query.appendWhere(sc, new int[] { 0 });

		System.out.println("query " + query.toString());
		QueryResult results = PersistenceHelper.manager.find(query);
		while (results.hasMoreElements()) {
			Object obj[] = (Object[]) results.nextElement();
			ClassHeader ch = (ClassHeader) obj[0];
			result = ch.getH2_PREFIX_DESC();
		}
		System.out.println("result: " + result);

		return result;
	}

	// ------------------------------------------------------------------------------------------------
	// 특수 목적 채번
	//
    public String actionSpecialSerial(List<Map<String, Object>> datas, 
                                    Map<String, Object> result) throws Exception {
        
        String matnr = extractMatnr(result);
        Transaction transaction = null;
        
        try {
            transaction = new Transaction();
            transaction.start();
            
            for (Map<String, Object> data : datas) {
                processSpecialSerial(data, matnr);
            }
            
            transaction.commit();
            return "message: 성공";
            
        } catch (Exception e) {
            rollbackTransaction(transaction);
            System.out.println("특수 채번 처리 중 오류 발생" + e);
            throw e;
        }
    }
    
    /**
     * MATNR 값 추출 및 검증
     */
    private String extractMatnr(Map<String, Object> result) throws Exception {
        String matnr = Objects.toString(result.get("MATNR"), ERROR_MATNR);
        
        if (ERROR_MATNR.equals(matnr)) {
            throw new Exception("MATNR 값이 유효하지 않습니다.");
        }
        
        return matnr;
    }
    
    /**
     * 개별 특수 채번 처리
     */
    private void processSpecialSerial(Map<String, Object> data, String matnr) throws Exception {
        String classification = Objects.toString(data.get("Class"), "");
        String partOID = Objects.toString(data.get("partOID"), "");
        
        WTPart originalPart = getWTPart(partOID);
        Map<String, Object> attributes = extractPartAttributes(partOID, classification);
        
        WTPart newPart = createNewWTPart(originalPart, attributes, classification, matnr);
        saveAndUpdateLifecycle(newPart);
    }
    
    /**
     * WTPart 속성 추출
     */
    private Map<String, Object> extractPartAttributes(String partOID, String classification) throws Exception {
        Map<String, Object> editMap = new HashMap<>();
        Map<String, Object> editMapAction = new HashMap<>();
        
        editMapAction.put("partOID", partOID);
        editMapAction.put("Class", classification);
        editMap.put("1", editMapAction);
        
        Map<String, Object> attributes = getWTPartToEdit(editMap);
        
        // 불필요한 속성 제거
        attributes.entrySet().removeIf(entry -> EXCLUDED_ATTRIBUTES.contains(entry.getKey()));
        
        logAttributes(attributes);
        return attributes;
    }
    
    /**
     * 새로운 WTPart 생성
     */
    private WTPart createNewWTPart(WTPart originalPart, Map<String, Object> attributes, 
                                  String classification, String matnr) throws Exception {
        
        ClassHeader classHeader = getFindClassHeader(classification);
        List<Map<String, Object>> classItems = getFindClassItem(classification);
        
        String partName = generatePartName(classHeader, classItems, attributes);
        
        WTPart newPart = WTPart.newWTPart();
        
        // 기본 정보 설정
        setBasicInfo(newPart, matnr, partName);
        
        // 분류 정보 설정
        setClassification(newPart, classification);
        
        // 단위 및 소유자 설정
        setUnitAndOwnership(newPart, originalPart);
        
        // 컨테이너 및 폴더 설정
        setContainerAndFolder(newPart, originalPart);
        
        // 라이프사이클 설정
        setLifecycle(newPart, originalPart.getContainer());
        
        // 뷰 할당
        assignView(newPart);
        
        return newPart;
    }
    
    /**
     * 부품명 생성
     */
    private String generatePartName(ClassHeader classHeader, 
                                   List<Map<String, Object>> classItems, 
                                   Map<String, Object> attributes) {
        
        StringBuilder nameBuilder = new StringBuilder();
        
        // Prefix 추가
        String prefix = classHeader.getH2_PREFIX_DESC();
        if (!prefix.isEmpty()) {
            nameBuilder.append(prefix);
        }
        
        // 속성값 추가
        for (Map<String, Object> item : classItems) {
            String attrName = (String) item.get("H2_ATBEZ");
            Object value = attributes.get(attrName);
            
            if (value != null) {
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(":");
                }
                nameBuilder.append(value.toString());
            }
        }
        
        String name = nameBuilder.toString();
        
        // 이름 길이 제한
        if (name.length() > MAX_NAME_LENGTH1) {
            name = name.substring(0, MAX_NAME_LENGTH1);
        }
        
        return name.isEmpty() ? DEFAULT_NAME : name;
    }
    
    /**
     * 기본 정보 설정
     */
    private void setBasicInfo(WTPart part, String number, String name) {
        try {
			part.setNumber(number);
			part.setName(name);
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 분류 정보 설정
     */
    private void setClassification(WTPart part, String classification) {
        WTPartTypeInfo partTypeInfo = new WTPartTypeInfo();
        try {
			partTypeInfo.setPtc_str_1(classification);
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
        part.setTypeInfoWTPart(partTypeInfo);
    }
    
    /**
     * 단위 및 소유자 설정
     */
    private void setUnitAndOwnership(WTPart newPart, WTPart originalPart) throws Exception {
        newPart.setDefaultUnit(originalPart.getDefaultUnit());
        
        WTPrincipal principal = SessionHelper.manager.getPrincipal();
        newPart.setOwnership(Ownership.newOwnership(principal));
    }
    
    /**
     * 컨테이너 및 폴더 설정
     */
    private void setContainerAndFolder(WTPart newPart, WTPart originalPart) throws Exception {
        WTContainer container = originalPart.getContainer();
        WTContainerRef containerRef = WTContainerRef.newWTContainerRef(container);
        newPart.setContainerReference(containerRef);
        
        String folderPath = getFolderPath(originalPart);
        Folder folder = FolderHelper.service.getFolder(folderPath, containerRef);
        FolderHelper.assignLocation((FolderEntry) newPart, folder);
    }
    
    /**
     * 폴더 경로 추출
     */
    private String getFolderPath(WTPart part) {
        String folderPath = part.getFolderingInfo().getLocation();
        
        if (folderPath == null || folderPath.trim().isEmpty()) {
            folderPath = DEFAULT_FOLDER_PATH;
        }
        
        return folderPath;
    }
    
    /**
     * 라이프사이클 설정
     */
    private void setLifecycle(WTPart part, WTContainer container) throws Exception {
        WTContainerRef containerRef = WTContainerRef.newWTContainerRef(container);
        LifeCycleTemplateReference lcRef = LifeCycleHelper.service
            .getLifeCycleTemplateReference(LIFECYCLE_TEMPLATE, containerRef);
        LifeCycleHelper.setLifeCycle(part, lcRef);
    }
    
    /**
     * 뷰 할당
     */
    private void assignView(WTPart part) throws Exception {
        View view = ViewHelper.service.getView(VIEW_NAME);
        ViewHelper.assignToView(part, view);
    }
    
    /**
     * 부품 저장 및 라이프사이클 업데이트
     */
    private void saveAndUpdateLifecycle(WTPart part) throws Exception {
        part = (WTPart) PersistenceHelper.manager.save(part);
        part = (WTPart) PersistenceHelper.manager.refresh(part);
        part = (WTPart) CommonUtils.actionPartAndEPMLifeCycleUpdate(part);
        LifeCycleHelper.service.setLifeCycleState(part, State.toState("CR"));
    }
    
    /**
     * 클래스 아이템 조회
     */
    public List<Map<String, Object>> getFindClassItem(String classification) throws Exception {
        QuerySpec query = buildClassItemQuery(classification);
        return executeClassItemQuery(query);
    }
    
    /**
     * 클래스 아이템 쿼리 생성
     */
    private QuerySpec buildClassItemQuery(String classification) throws Exception {
        QuerySpec query = new QuerySpec();
        int idx = query.appendClassList(ClassItem.class, true);
        
        // Class 조건
        SearchCondition classCondition = new SearchCondition(
            ClassItem.class, "H2_CLASS", SearchCondition.EQUAL, classification
        );
        query.appendWhere(classCondition, new int[] { idx });
        
        // DESC_FLAG 조건
        query.appendAnd();
        SearchCondition flagCondition = new SearchCondition(
            ClassItem.class, "H2_DESC_FLAG", SearchCondition.EQUAL, DESC_FLAG_VALUE
        );
        query.appendWhere(flagCondition, new int[] { idx });
        
        // 정렬 조건
        ClassAttribute orderAttr = new ClassAttribute(ClassItem.class, ClassItem.H2__DESC__ORDER);
        query.appendOrderBy(new OrderBy(orderAttr, false), new int[] { idx });
        
        return query;
    }
    
    /**
     * 클래스 아이템 쿼리 실행
     */
    private List<Map<String, Object>> executeClassItemQuery(QuerySpec query) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        QueryResult queryResult = PersistenceHelper.manager.find(query);
        
        while (queryResult.hasMoreElements()) {
            Object[] row = (Object[]) queryResult.nextElement();
            ClassItem item = (ClassItem) row[0];
            
            Map<String, Object> data = new HashMap<>();
            data.put("H2_ATBEZ", item.getH2_ATBEZ());
            data.put("H2_DESC_ORDER", item.getH2_DESC_ORDER());
            
            results.add(data);
        }
        
        return results;
    }
    
    /**
     * 속성 로깅
     */
    private void logAttributes(Map<String, Object> attributes) {
        attributes.forEach((key, value) -> 
            System.out.println("속성 확인: " + key + " = " + value)
        );
    }
    
    private WTPart getWTPart(String partOID) throws Exception {
        return (WTPart) getClasszz(partOID);
    }

	// select * from classitem where h2_class= 'CL51000008';
	public List<Map<String, Object>> getFindClassItems(String Class) throws Exception {
		List<Map<String, Object>> datas = new ArrayList<>();
		QuerySpec query = new QuerySpec();
		int idx = query.appendClassList(ClassItem.class, true);
		SearchCondition sc = new SearchCondition();
		// Class 조건 추가
		sc = new SearchCondition(ClassItem.class, "H2_CLASS", SearchCondition.EQUAL, Class);
		query.appendWhere(sc, new int[] { idx });
		query.appendAnd();
		sc = new SearchCondition(ClassItem.class, "H2_DESC_FLAG", SearchCondition.EQUAL, "X");
		query.appendWhere(sc, new int[] { idx });

		// order by H2_DESC_ORDER
		ClassAttribute ca = new ClassAttribute(ClassItem.class, ClassItem.H2__DESC__ORDER);
		query.appendOrderBy(new OrderBy(ca, false), new int[] { idx });

		System.out.println("query " + query.toString());
		QueryResult results = PersistenceHelper.manager.find(query);

		while (results.hasMoreElements()) {
			Object obj[] = (Object[]) results.nextElement();
			ClassItem cl = (ClassItem) obj[0]; // ClassItem 데이터가 2번째 인덱스
			Map<String, Object> data = new HashMap<>();
			data.put("H2_ATBEZ", cl.getH2_ATBEZ());
			data.put("H2_DESC_ORDER", cl.getH2_DESC_ORDER());

			datas.add(data);
		}
		return datas;
	}

	public ClassHeader getFindClassHeader(String Class) throws Exception {

		try {
			QuerySpec query = new QuerySpec();
			int idx = query.appendClassList(ClassHeader.class, true);
			SearchCondition sc = new SearchCondition(ClassHeader.class, "H2_CLASS", SearchCondition.EQUAL, Class);
			query.appendWhere(sc, new int[] { idx });

			QueryResult results = PersistenceHelper.manager.find(query);
			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ClassHeader cl = (ClassHeader) obj[0]; // ClassItem 데이터가 2번째 인덱스
				// data = cl.getH2_IS_3S();
				return cl;
			}

		} catch (Exception e) {
			throw new Exception("message: Find Class Header Error");
		}
		return null;
	}

	public List<ClassItem> getFindAllClassItems(String Class) throws Exception {
		try {
			QuerySpec query = new QuerySpec();
			int idx = query.appendClassList(ClassItem.class, true);
			SearchCondition sc = new SearchCondition(ClassItem.class, "H2_CLASS", SearchCondition.EQUAL, Class);
			query.appendWhere(sc, new int[] { idx });

			QueryResult results = PersistenceHelper.manager.find(query);
			List<ClassItem> list = new ArrayList<>();
			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ClassItem cl = (ClassItem) obj[0];
				list.add(cl);
			}
			return list;
		} catch (Exception e) {
			throw new Exception("message: Find Class Item Error");
		}
	}

	String buildOid(String classname, long ida2a2) {
		return classname + ":" + ida2a2;
	}

	public String strOutput(String input) {
		String output = input.replaceAll("[^a-zA-Z0-9._]", "_");
		return output;
	}

	public String messageChange(Map<String, Object> params, Object result) throws Exception {
		// partSerialListOID:"com.hyosung.tnsplm.serial.entity.WTPartSerialList:420934"
		String oid = Objects.toString(params.get("partSerialListOID"), "");
		if (oid.equals("")) {
			return "E";
		}
		Transaction tnx = null;
		try {
			tnx = new Transaction();
			tnx.start();
			ReferenceFactory rf = new ReferenceFactory();
			Persistable persistable = null;
			WTPartSerialList wtpsl = null;

			persistable = rf.getReference(oid).getObject();
			if (persistable instanceof WTPartSerialList) {
				wtpsl = (WTPartSerialList) persistable;
				persistable = null;
			}

			if (wtpsl == null) {
				return "E";
			}
			Map<String, Object> resultMap = null;
			if (result instanceof Map) {
				resultMap = (Map<String, Object>) result;
			}
			// Objects.toString(data.get("MESSAGE"), "MD Interface Error: MD → PLM 채번 검증
			// 실패");
			String message = Objects.toString(resultMap.get("MESSAGE"), "").trim();
			if (message.isEmpty()) {
				message = "MD Interface:  MD → PLM 전달된 메시지(MESSAGE) 없음";
			}
			wtpsl.setH2_IF_MESSAGE("message: " + message);
			String resultCheck = Objects.toString(resultMap.get("RESULT"), "").trim();
			wtpsl.setH2_TARGET_REGISTER_DATE(timestamp);
			wtpsl.setH2_IF_SUCCESS_DIV("S".equalsIgnoreCase(resultCheck) ? "Success" : "Error");

			PersistenceHelper.manager.modify(wtpsl);

			tnx.commit();
			tnx = null;
		} catch (Exception e) {
			if (tnx != null) {
				tnx.rollback();
			}

			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			throw e;

		}
		return "S";
	}

	// zmdmclassmaterialvalue get find
	public String getFindZmdmClassMaterialValue(String Class) throws Exception {
		try {
			if (Class == null || Class.toString().trim().isEmpty()) {
				throw new Exception("message: NOT CLASS");
			}

			QuerySpec query = new QuerySpec();
			int idx = query.appendClassList(ZMdmClassMaterialValue.class, true);
			SearchCondition sc = new SearchCondition(ZMdmClassMaterialValue.class, ZMdmClassMaterialValue.CLAZZ,
					SearchCondition.EQUAL, Class);
			query.appendWhere(sc, new int[] { idx });

			QueryResult results = PersistenceHelper.manager.find(query);
			while (results.hasMoreElements()) {
				Object obj[] = (Object[]) results.nextElement();
				ZMdmClassMaterialValue zcmv = (ZMdmClassMaterialValue) obj[0];
				return zcmv.getPrdha();
			}

			return "";
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	//------------------------------------------------------------------------------------------------------------------------
	//
	//
	
	/**
	 * 유효한 속성 파라미터 추출
	 */
	private Map<String, Object> extractValidAttributeParams(Map<String, Object> params) {
	    Map<String, Object> attributeParams = new HashMap<>();
	    
	    for (Map.Entry<String, Object> entry : params.entrySet()) {
	        String key = entry.getKey();
	        Object value = entry.getValue();
	        
	        // "Class"는 제외하고, null이 아니고 빈 값이 아닌 것만 포함
	        if (!"Class".equalsIgnoreCase(key) && value != null && 
	            !value.toString().trim().isEmpty()) {
	            attributeParams.put(key, value);
	        }
	    }
	    
	    return attributeParams;
	}
	
	/**
	 * 속성 기반 유사 EPMDocument 검색 (안전한 버전)
	 */
	public List<Map<String, Object>> getSimilarEPMDocumentByAttributes(Map<String, Object> params) throws Exception {
	    List<Map<String, Object>> resultList = new ArrayList<>();

	    try {
	        // 1. 입력 파라미터 검증
	        String clazz = Objects.toString(params.get("Class"), "").trim();
	        if (clazz.isEmpty()) {
	        	//throw new Exception("Class parameter is required");
	        	System.out.println("Class parameter is required");
	            throw new Exception("선택된 분류 체계가 없습니다.");
	        }

	        // 2. 동적 속성 파라미터 추출
	        Map<String, Object> attributeParams = extractValidAttributeParams(params);
	        if (attributeParams.isEmpty()) {
	            System.out.println("No valid attribute parameters found");
	            throw new Exception("검색 조건이 빈 값입니다.");
	            //return resultList;
	        }
	        
	        // 검색 조건 출력
	        System.out.println("=== 검색 조건 ===");
	        System.out.println("Class: " + clazz);
	        System.out.println("Search Attributes:");
	        for (Map.Entry<String, Object> entry : attributeParams.entrySet()) {
	            System.out.println("  - " + entry.getKey() + " = " + entry.getValue());
	        }
	        System.out.println("================");

	        // 3. 안전한 단계별 검색 실행
	        resultList = executeSafeSearch(clazz, attributeParams);
	        
	        System.out.println("Found " + resultList.size() + " EPMDocuments");

	    } catch (Exception e) {
	        System.err.println("Error in getSimilarEPMDocumentByAttributes: " + e.getMessage());
	        e.printStackTrace();
	        //throw new Exception("Failed to search EPMDocuments: " + e.getMessage());
	        throw new Exception("message: " + e.getMessage());
	    }

	    return resultList;
	}

	/**
	 * 안전한 검색 실행 - 각 단계를 독립적으로 처리
	 */
	private List<Map<String, Object>> executeSafeSearch(String clazz, Map<String, Object> attributeParams) throws Exception {
	    List<Map<String, Object>> resultList = new ArrayList<>();
	    
	    try {
	        // Step 1: WTPart 목록 먼저 조회 (클래스 필터링 포함)
	        List<WTPart> candidateParts = getCandidatePartsByClass(clazz);
	        
	        if (candidateParts.isEmpty()) {
	            System.out.println("No candidate Parts found for class: " + clazz);
	            return resultList;
	        }
	        
	        System.out.println("Found " + candidateParts.size() + " candidate parts");
	        
	        // Step 2: 각 Part에 대해 연관된 EPMDocument 조회 및 속성 매칭
	        for (WTPart part : candidateParts) {
	            try {
	                EPMDocument epmDoc = getEPMDocumentByWTPart(part);
	                if (epmDoc != null) {
	                    // 속성 매칭 확인
	                    if (isPartMatchingAttributes(part, epmDoc, clazz, attributeParams)) {
	                        Map<String, Object> resultMap = createDetailedResultMap(part, epmDoc, clazz, attributeParams);
	                        resultList.add(resultMap);
	                    }
	                }
	            } catch (Exception e) {
	                System.err.println("Error processing part " + part.getNumber() + ": " + e.getMessage());
	                // 개별 Part 처리 실패는 무시하고 계속 진행
	                continue;
	            }
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Error in safe search execution: " + e.getMessage());
	        throw e;
	    }
	    
	    return resultList;
	}

	/**
	 * 클래스별 후보 Part 목록 조회 (단순한 쿼리만 사용)
	 */
	private List<WTPart> getCandidatePartsByClass(String clazz) throws Exception {
	    List<WTPart> partList = new ArrayList<>();
	    
	    try {
	        // 가장 단순한 WTPart 쿼리
	        QuerySpec query = new QuerySpec();
	        int idx = query.appendClassList(WTPart.class, true);
	        
	        // 기본 조건만 설정
	        query.appendWhere(new SearchCondition(WTPart.class, "checkoutInfo.state", 
	            new String[]{"c/i", "c/o"}, true, false), new int[]{idx});
	        query.appendAnd();
	        query.appendWhere(new SearchCondition(WTPart.class, Iterated.LATEST_ITERATION, 
	            SearchCondition.IS_TRUE), new int[]{idx});
	        
	        System.out.println("Simple Part Query: " + query.toString());
	        
	        QueryResult qr = PersistenceHelper.manager.find(query);
	        
	        while (qr.hasMoreElements()) {
	        	Object[] obj = (Object[]) qr.nextElement();
	            WTPart part = (WTPart) obj[0];
	            
	            // 클래스 필터링을 메모리에서 수행
	            try {
	                if (part.getTypeInfoWTPart() != null && 
	                    clazz.equals(part.getTypeInfoWTPart().getPtc_str_1())) {
	                    partList.add(part);
	                }
	            } catch (Exception e) {
	                System.err.println("Error checking part class for " + part.getNumber() + ": " + e.getMessage());
	                continue;
	            }
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Error getting candidate parts: " + e.getMessage());
	        throw new Exception("Failed to get candidate parts: " + e.getMessage());
	    }
	    
	    return partList;
	}

	/**
	 * Part가 속성 조건과 매칭되는지 확인
	 * params의 모든 속성 값이 자재의 실제 속성 값과 일치하는지 검사
	 */
	private boolean isPartMatchingAttributes(WTPart part, EPMDocument epmDoc, String clazz, 
	                                       Map<String, Object> attributeParams) {
	    try {
	        // 속성 타입 정보 조회
	        Map<String, String> attributeTypes = getAttributeTypesSimple(clazz, attributeParams.keySet());
	        
	        int matchedCount = 0;
	        int totalSearchCount = attributeParams.size();
	        
	        for (Map.Entry<String, Object> entry : attributeParams.entrySet()) {
	            String attrName = entry.getKey();
	            Object expectedValue = entry.getValue();
	            String attrType = attributeTypes.get(attrName);
	            
	            if (attrType == null) {
	                System.out.println("Warning: Unknown attribute type for " + attrName + " in class " + clazz);
	                continue;
	            }
	            
	            Object actualValue = null;
	            
	            // 속성 타입에 따라 실제 값 조회
	            if ("GR".equals(attrType)) {
	                actualValue = getGRAttributeValueSimple(epmDoc, attrName);
	            } else if ("X".equals(attrType)) {
	                actualValue = getXAttributeValueSimple(clazz, part, attrName);
	            } else if ("LR".equals(attrType)) {
	                actualValue = getGRAttributeValueSimple(epmDoc, attrName); // LR도 GR과 동일하게 처리
	            }
	            
	            // 값 매칭 확인
	            if (actualValue != null && isValueMatching(expectedValue, actualValue)) {
	                matchedCount++;
	                System.out.println("✓ Matched attribute [" + attrName + "]: expected=" + expectedValue + ", actual=" + actualValue);
	            } else {
	                System.out.println("✗ No match for attribute [" + attrName + "]: expected=" + expectedValue + ", actual=" + actualValue);
	                // 하나라도 매칭되지 않으면 해당 자재는 제외
	                return false;
	            }
	        }
	        
	        // 모든 검색 조건이 매칭되어야 true 반환
	        boolean isFullMatch = (matchedCount == totalSearchCount && totalSearchCount > 0);
	        
	        if (isFullMatch) {
	            System.out.println("✅ Part " + part.getNumber() + " matches ALL attributes (" + matchedCount + "/" + totalSearchCount + ")");
	        }
	        
	        return isFullMatch;
	        
	    } catch (Exception e) {
	        System.err.println("Error matching attributes for part " + part.getNumber() + ": " + e.getMessage());
	        return false;
	    }
	}

	/**
	 * 단순한 속성 타입 조회
	 */
	private Map<String, String> getAttributeTypesSimple(String clazz, Set<String> attributeNames) {
	    Map<String, String> attributeTypes = new HashMap<>();
	    
	    for (String attrName : attributeNames) {
	        try {
	            String attrType = getSingleAttributeTypeSimple(clazz, attrName);
	            if (attrType != null) {
	                attributeTypes.put(attrName, attrType);
	            }
	        } catch (Exception e) {
	            System.err.println("Error getting attribute type for " + attrName + ": " + e.getMessage());
	        }
	    }
	    
	    return attributeTypes;
	}

	/**
	 * 개별 속성 타입 조회 (단순한 쿼리)
	 */
	private String getSingleAttributeTypeSimple(String clazz, String attrName) {
	    try {
	        // ObjectClassificationMapping을 직접 조회
	        QuerySpec query = new QuerySpec();
	        int idx = query.appendClassList(ObjectClassificationMapping.class, true);
	        
	        query.appendWhere(new SearchCondition(ObjectClassificationMapping.class, 
	            ObjectClassificationMapping.H2__PLM__PROP, SearchCondition.EQUAL, attrName), 
	            new int[]{idx});
	        
	        QueryResult qr = PersistenceHelper.manager.find(query);
	        
	        while (qr.hasMoreElements()) {
	        	Object[] obj = (Object[]) qr.nextElement();
	            ObjectClassificationMapping mapping = (ObjectClassificationMapping) obj[0];
	            
	            // 해당 속성이 지정된 클래스와 연관되어 있는지 확인
	            if (isAttributeRelatedToClass(mapping.getH2_ATNAM(), clazz)) {
	                return mapping.getH2_PROP_TYPE();
	            }
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Error getting single attribute type for " + attrName + ": " + e.getMessage());
	    }
	    
	    return null;
	}

	/**
	 * 속성이 클래스와 연관되어 있는지 확인
	 */
	private boolean isAttributeRelatedToClass(String atnam, String clazz) {
	    try {
	        QuerySpec query = new QuerySpec();
	        int idx = query.appendClassList(ClassItem.class, true);
	        
	        query.appendWhere(new SearchCondition(ClassItem.class, ClassItem.H2__CLASS, 
	            SearchCondition.EQUAL, clazz), new int[]{idx});
	        query.appendAnd();
	        query.appendWhere(new SearchCondition(ClassItem.class, ClassItem.H2__ATNAM, 
	            SearchCondition.EQUAL, atnam), new int[]{idx});
	        
	        QueryResult qr = PersistenceHelper.manager.find(query);
	        return qr.hasMoreElements();
	        
	    } catch (Exception e) {
	        System.err.println("Error checking attribute-class relation: " + e.getMessage());
	        return false;
	    }
	}

	/**
	 * 단순한 GR 속성 값 조회
	 */
	private Object getGRAttributeValueSimple(EPMDocument epmDoc, String attrName) {
	    try {
	        // IBAUtils를 사용한 간단한 속성 값 조회
	        String stringValue = IBAUtils.getStringValue(epmDoc, attrName);
	        if (stringValue != null && !stringValue.trim().isEmpty()) {
	            return stringValue.trim();
	        }
	        
	        float floatValue = IBAUtils.getFloatValue(epmDoc, attrName);
	        if (floatValue != 0f) {
	            return floatValue;
	        }
	        
	        Boolean booleanValue = IBAUtils.getBooleanValue(epmDoc, attrName);
	        if (booleanValue != null) {
	            return booleanValue;
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Error getting GR attribute " + attrName + ": " + e.getMessage());
	    }
	    
	    return null;
	}

	/**
	 * 단순한 X 속성 값 조회
	 */
	private String getXAttributeValueSimple(String clazz, WTPart part, String attrName) {
	    try {
	        // 기존 getStringValues 메서드 활용
	        ObjectClassificationMapping ocm = getObjClassMapping(clazz, attrName);
	        if (ocm != null) {
	            return getStringValues(false, ocm, part, part.getMaster());
	        }
	    } catch (Exception e) {
	        System.err.println("Error getting X attribute " + attrName + ": " + e.getMessage());
	    }
	    
	    return null;
	}

	/**
	 * 값 매칭 확인 (개선된 버전)
	 */
	private boolean isValueMatching(Object expected, Object actual) {
	    if (expected == null && actual == null) {
	        return true;
	    }
	    
	    if (expected == null || actual == null) {
	        return false;
	    }
	    
	    // 문자열 비교
	    String expectedStr = expected.toString().trim();
	    String actualStr = actual.toString().trim();
	    
	    // 정확히 일치하는 경우
	    if (expectedStr.equals(actualStr)) {
	        return true;
	    }
	    
	    // 대소문자 무시하고 비교
	    if (expectedStr.equalsIgnoreCase(actualStr)) {
	        return true;
	    }
	    
	    // 숫자인 경우 숫자로 비교
	    try {
	        double expectedNum = Double.parseDouble(expectedStr);
	        double actualNum = Double.parseDouble(actualStr);
	        return Math.abs(expectedNum - actualNum) < 0.001; // 부동소수점 오차 고려
	    } catch (NumberFormatException e) {
	        // 숫자가 아닌 경우 무시
	    }
	    
	    return false;
	}

	/**
	 * 상세한 결과 Map 생성
	 */
	private Map<String, Object> createDetailedResultMap(WTPart part, EPMDocument epmDoc, String clazz, 
	                                                   Map<String, Object> attributeParams) {
	    Map<String, Object> resultMap = new HashMap<>();
	    
	    try {
	        EPMDocumentMaster docMaster = (EPMDocumentMaster) epmDoc.getMaster();
	        
	        // 기본 정보
	        ClassHeader ch = getFindClassHeader(clazz);
	        resultMap.put("partNumber", part.getNumber());
	        resultMap.put("partName", part.getName());
	        resultMap.put("thumbnail",  CommonUtils.thumbnails(part.getPersistInfo().getObjectIdentifier().getStringValue())[0]);
	        resultMap.put("documentNumber", epmDoc.getNumber());
	        resultMap.put("documentName", epmDoc.getName());
	        resultMap.put("cadName", docMaster.getCADName());
	        resultMap.put("documentType", epmDoc.getDocType().toString());
	        resultMap.put("createdBy", epmDoc.getCreatorFullName());
	        resultMap.put("createdTime", epmDoc.getCreateTimestamp());
	        resultMap.put("modifiedTime", epmDoc.getModifyTimestamp());
	        resultMap.put("status", part.getState().toString());
	        
	        String version = epmDoc.getVersionIdentifier().getValue() + "." + 
	                        epmDoc.getIterationIdentifier().getValue();
	        resultMap.put("version", version);
	        
	        resultMap.put("epmDocumentOID", epmDoc.getPersistInfo().getObjectIdentifier().getStringValue());
	        resultMap.put("partOID", part.getPersistInfo().getObjectIdentifier().getStringValue());
	        resultMap.put("Class", clazz);
	        resultMap.put("className",  ch != null ? ch.getH2_KSCHL() : "");
	        
	        // 매칭된 속성 정보만 추가 (검색 조건으로 사용된 것들)
	        Map<String, Object> matchedAttributes = new HashMap<>();
	        Map<String, String> attributeTypes = getAttributeTypesSimple(clazz, attributeParams.keySet());
	        
	        for (Map.Entry<String, Object> paramEntry : attributeParams.entrySet()) {
	            String attrName = paramEntry.getKey();
	            Object searchValue = paramEntry.getValue();
	            
	            try {
	                String attrType = attributeTypes.get(attrName);
	                Object actualValue = null;
	                
	                if ("GR".equals(attrType) || "LR".equals(attrType)) {
	                    actualValue = getGRAttributeValueSimple(epmDoc, attrName);
	                } else if ("X".equals(attrType)) {
	                    actualValue = getXAttributeValueSimple(clazz, part, attrName);
	                }
	                
	                // 매칭된 속성만 결과에 포함
	                if (actualValue != null && isValueMatching(searchValue, actualValue)) {
	                    Map<String, Object> attrInfo = new HashMap<>();
	                    attrInfo.put("searchValue", searchValue);
	                    attrInfo.put("actualValue", actualValue);
	                    attrInfo.put("type", attrType);
	                    matchedAttributes.put(attrName, attrInfo);
	                }
	            } catch (Exception e) {
	                System.err.println("Error processing attribute " + attrName + " for result: " + e.getMessage());
	            }
	        }
	        
	        resultMap.put("matchedAttributes", matchedAttributes);
	        resultMap.put("matchedCount", matchedAttributes.size());
	        resultMap.put("totalSearchAttributes", attributeParams.size());
	        
	    } catch (Exception e) {
	        System.err.println("Error creating detailed result map: " + e.getMessage());
	        resultMap.put("error", e.getMessage());
	    }
	    
	    return resultMap;
	}
	
	//
	//
	//------------------------------------------------------------------------------------------------------------------------
	
	
	//------------------------------------------------------------------------------------------------------------------------
	//
	//
	    /**
	     * 채번 승인 처리 메인 메서드
	     * 
	     * @param datas 처리할 채번 데이터 목록
	     * @param result SAP에서 받은 채번 결과 데이터
	     * @return 처리 결과 메시지
	     * @throws Exception 전체 처리가 불가능한 경우에만 예외 발생
	     */
	    public String actionSerialApproval(List<Map<String, Object>> datas, Object result) throws Exception {
	        validateInputs(datas, result);
	        
	        System.out.println("=== 채번 승인 처리 시작 (건별 독립 처리) ===");
	        System.out.println("처리 대상: " + datas.size() + "건");
	        
	        // 메인 트랜잭션 없이 건별 독립 처리
	        try {
	            processSerialApprovalItems(datas, result);
	            
	            System.out.println("=== 채번 승인 처리 완료 ===");
	            return "success";
	            
	        } catch (Exception e) {
	            System.err.println("채번 승인 처리 중 심각한 오류 발생: " + e.getMessage());
	            // 건별 처리에서는 개별 실패가 전체 실패를 의미하지 않음
	            // 단, 시스템 레벨 오류인 경우에만 예외 발생
	            throw e;
	        }
	    }
	    
	    /**
	     * 입력 파라미터 검증
	     */
	    private void validateInputs(List<Map<String, Object>> datas, Object result) throws Exception {
	        if (datas == null || datas.isEmpty()) {
	            throw new IllegalArgumentException("처리할 데이터가 없습니다.");
	        }
	        if (result == null) {
	            throw new IllegalArgumentException("채번 결과 데이터가 없습니다.");
	        }
	    }
	    
	    /**
	     * 트랜잭션 롤백 처리
	     */
	    private void rollbackTransaction(Transaction transaction) {
	        if (transaction != null) {
	            try {
	                transaction.rollback();
	            } catch (Exception rollbackEx) {
	                System.err.println("트랜잭션 롤백 실패: " + rollbackEx.getMessage());
	            }
	        }
	    }
	    
	    /**
	     * 채번 승인 항목들 순차 처리 (건별 독립 처리)
	     */
	    private void processSerialApprovalItems(List<Map<String, Object>> datas, Object result) throws Exception {
	        int successCount = 0;
	        int failCount = 0;
	        
	        for (int i = 0; i < datas.size(); i++) {
	            Map<String, Object> data = datas.get(i);
	            System.out.println("처리 중: " + (i + 1) + "/" + datas.size());
	            
	            try {
	                SerialApprovalContext context = createSerialApprovalContext(data, result);
	                processSerialApprovalItemIndividually(context);
	                successCount++;
	                System.out.println("항목 처리 성공 [" + (i + 1) + "]: " + context.getPartNumber());
	                
	            } catch (Exception e) {
	                failCount++;
	                System.err.println("항목 처리 실패 [" + (i + 1) + "]: " + e.getMessage());
	                
	                // 개별 항목 실패시에도 상태는 기록
	                try {
	                    handleIndividualItemFailure(data, result, e);
	                } catch (Exception statusEx) {
	                    System.err.println("실패 상태 기록 중 오류 [" + (i + 1) + "]: " + statusEx.getMessage());
	                }
	            }
	        }
	        
	        System.out.println("=== 처리 결과 요약 ===");
	        System.out.println("전체: " + datas.size() + "건, 성공: " + successCount + "건, 실패: " + failCount + "건");
	    }
	    
	    /**
	     * 채번 승인 컨텍스트 생성
	     */
	    private SerialApprovalContext createSerialApprovalContext(Map<String, Object> data, Object result) throws Exception {
	        SerialApprovalContext context = new SerialApprovalContext(data);
	        context.loadWindchillObjects();
	        context.findMatchedResult(result);
	        return context;
	    }
	    
	    /**
	     * 개별 채번 승인 항목 처리 (독립적 트랜잭션)
	     */
	    private void processSerialApprovalItemIndividually(SerialApprovalContext context) throws Exception {
	        Transaction itemTransaction = null;
	        try {
	            // 개별 항목용 트랜잭션 시작
	            itemTransaction = new Transaction();
	            itemTransaction.start();
	            
	            WTCollection processedItems = new WTArrayList();
	            
	            // 1. Part 채번 처리
	            processPartNumbering(context);
	            
	            // 2. EPMDocument 채번 처리 (선택적)
	            if (context.hasEPMDocument()) {
	                processEPMDocumentNumbering(context);
	            }
	            
	            // 3. 승인 요청 데이터 정리
	            //cleanupApprovalRequestData(context);
	            
	            // 4. 상태 업데이트 (성공)
	            updateProcessingStatusIndividually(context, true, "", processedItems);
	            
	            // 개별 트랜잭션 커밋
	            itemTransaction.commit();
	            
	        } catch (Exception e) {
	            // 개별 트랜잭션 롤백
	            if (itemTransaction != null) {
	                try {
	                    itemTransaction.rollback();
	                } catch (Exception rollbackEx) {
	                    System.err.println("개별 트랜잭션 롤백 실패: " + rollbackEx.getMessage());
	                }
	            }
	            throw e; // 상위로 예외 전파하여 실패 상태 기록 처리
	        }
	    }
	    
	    /**
	     * 개별 항목 실패 처리
	     */
	    private void handleIndividualItemFailure(Map<String, Object> data, Object result, Exception e) throws Exception {
	        Transaction failureTransaction = null;
	        try {
	            // 실패 상태 기록용 별도 트랜잭션
	            failureTransaction = new Transaction();
	            failureTransaction.start();
	            
	            // 최소한의 컨텍스트 생성 (객체 로드까지만)
	            SerialApprovalContext context = new SerialApprovalContext(data);
	            context.loadWindchillObjects();
	            
	            // 매칭 결과 찾기 (실패해도 무시)
	            try {
	                context.findMatchedResult(result);
	            } catch (Exception matchEx) {
	                System.err.println("매칭 결과 찾기 실패 (무시): " + matchEx.getMessage());
	            }
	            
	            WTCollection processedItems = new WTArrayList();
	            
	            // 실패 상태로 업데이트
	            updateProcessingStatusIndividually(context, false, e.getMessage(), processedItems);
	            
	            failureTransaction.commit();
	            
	        } catch (Exception statusEx) {
	            if (failureTransaction != null) {
	                try {
	                    failureTransaction.rollback();
	                } catch (Exception rollbackEx) {
	                    System.err.println("실패 처리 트랜잭션 롤백 실패: " + rollbackEx.getMessage());
	                }
	            }
	            throw statusEx;
	        }
	    }
	    
	    /**
	     * 개별 처리 상태 업데이트
	     */
	    private void updateProcessingStatusIndividually(SerialApprovalContext context, boolean isSuccess, 
	                                                  String errorMessage, WTCollection processedItems) throws Exception {
	        
	        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	        
	        try {
	            // PartSerialList 상태 업데이트
	            updatePartSerialListStatus(context, isSuccess, errorMessage, timestamp);
	            processedItems.add(context.partSerialList);
	            
	            // SerialList 상태 업데이트 (성공한 경우만)
	            if (context.serialList != null && isSuccess) {
	                context.serialList.setH2_REQ_STATUS("승인완료");
	                processedItems.add(context.serialList);
	            } else if (context.serialList != null) {
	                context.serialList.setH2_REQ_STATUS("승인실패");
	                processedItems.add(context.serialList);
	            }
	            
	            PersistenceHelper.manager.modify(processedItems);
	            
	        } catch (Exception e) {
	            System.err.println("개별 상태 업데이트 실패: " + e.getMessage());
	            throw new Exception("개별 상태 업데이트 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * Part 채번 처리
	     */
	    private void processPartNumbering(SerialApprovalContext context) throws Exception {
	        try {
	            System.out.println("Part 채번 처리 시작: " + context.getPartNumber());
	            
	            // 체크아웃
	            context.part = checkoutPart(context.part);
	            
	            // 번호/이름 변경
	            NumberingData numberingData = createPartNumberingData(context);
	            updatePartIdentity(context.part, numberingData);
	            
	            // 체크인 및 상태 변경
	            checkinPart(context.part);
	            setPartLifeCycleState(context.part, LIFECYCLE_STATE_RELEASED);
	            
	            System.out.println("Part 채번 처리 완료: " + numberingData.number);
	            
	        } catch (Exception e) {
	            System.err.println("Part 채번 실패: " + e.getMessage());
	            throw new Exception("Part 채번 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * Part 체크아웃
	     */
	    private WTPart checkoutPart(WTPart part) throws Exception {
	        return (WTPart) PartHelper.manager.getPartCheckOut(part, "채번 작업을 위한 체크아웃");
	    }
	    
	    /**
	     * Part 체크인
	     */
	    private void checkinPart(WTPart part) throws Exception {
	        WTCollection parts = new WTArrayList();
	        parts.add(part);
	        PartHelper.manager.checkinParts(parts, "채번 작업 완료");
	    }
	    
	    /**
	     * Part 생명주기 상태 설정
	     */
	    private void setPartLifeCycleState(WTPart part, String stateName) throws Exception {
	        WTPart refreshedPart = (WTPart) PersistenceHelper.manager.refresh(part);
	        LifeCycleHelper.service.setLifeCycleState(refreshedPart, State.toState(stateName));
	    }
	    
	    /**
	     * Part 채번 데이터 생성
	     */
	    private NumberingData createPartNumberingData(SerialApprovalContext context) {
	        String matnr = extractMATNR(context.matchedResult);
	        String originalName = context.part.getMaster().getName();
	        String adjustedName = adjustNameLength2(originalName, MAX_NAME_LENGTH);
	        
	        return new NumberingData(matnr, adjustedName);
	    }
	    
	    /**
	     * Part Identity 업데이트
	     */
	    private void updatePartIdentity(WTPart part, NumberingData data) throws Exception {
	    	try {
	            if (part == null) {
	                throw new IllegalArgumentException("Part 객체가 null입니다.");
	            }
	            
	            if (data == null || data.number == null || data.name == null) {
	                throw new IllegalArgumentException("NumberingData가 유효하지 않습니다.");
	            }
	            
	            System.out.println("Part Identity 업데이트 시작: " + part.getNumber() + " → " + data.number);
	            
	            WTPartMaster master = part.getMaster();
	            if (master == null) {
	                throw new IllegalStateException("Part Master가 null입니다.");
	            }
	            
	            // Identity 객체 생성 및 설정
	            WTPartMasterIdentity identity = (WTPartMasterIdentity) master.getIdentificationObject();
	            if (identity == null) {
	                throw new IllegalStateException("Part Master Identity가 null입니다.");
	            }
	            
	            // 기존 값 백업 (로깅용)
	            String oldNumber = identity.getNumber();
	            String oldName = identity.getName();
	            
	            // 새 값 설정
	            identity.setNumber(data.number);
	            identity.setName(data.name);
	            
	            // Master Identity 변경
	            IdentityHelper.service.changeIdentity(master, identity);
	            
	            // Part 이름도 변경 (체크아웃된 상태에서)
	            //part.setName(data.name);
	            
	            System.out.println("Part Identity 업데이트 완료:");
	            System.out.println("  번호: " + oldNumber + " → " + data.number);
	            System.out.println("  이름: " + oldName + " → " + data.name);
	            
	        } catch (Exception e) {
	            System.err.println("Part Identity 업데이트 실패: " + e.getMessage());
	            System.err.println("Part: " + (part != null ? part.getNumber() : "null"));
	            System.err.println("Data: " + (data != null ? data.toString() : "null"));
	            throw new Exception("Part Identity 업데이트 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * EPMDocument 채번 처리
	     */
	    private void processEPMDocumentNumbering(SerialApprovalContext context) throws Exception {
	        try {
	            System.out.println("EPMDocument 채번 처리 시작");
	            
	            NumberingData numberingData = createPartNumberingData(context);
	            
	            // 체크아웃
	            context.epmDocument = checkoutEPMDocument(context.epmDocument);
	            
	            // 3D 문서 처리
	            process3DDocument(context.epmDocument, numberingData);
	            
	            // 2D 문서 처리 (존재하는 경우)
	            EPMDocument epm2d = EPMHelper.manager.getEpm2D(context.epmDocument);
	            if (epm2d != null) {
	                process2DDocument(epm2d, numberingData);
	            }
	            
	            // 체크인 및 상태 변경
	            checkinEPMDocument(context.epmDocument);
	            setEPMDocumentLifeCycleState(context.epmDocument, LIFECYCLE_STATE_RELEASED);
	            
	            if (epm2d != null) {
	                setEPMDocumentLifeCycleState(epm2d, LIFECYCLE_STATE_RELEASED);
	            }
	            
	            System.out.println("EPMDocument 채번 처리 완료");
	            
	        } catch (Exception e) {
	            System.err.println("EPMDocument 채번 실패: " + e.getMessage());
	            throw new Exception("EPMDocument 채번 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * EPMDocument 체크아웃
	     */
	    private EPMDocument checkoutEPMDocument(EPMDocument epm) throws Exception {
	        Folder checkoutFolder = CheckInOutTaskLogic.getCheckoutFolder();
	        CheckoutLink checkoutLink = WorkInProgressHelper.service.checkout(
	            epm, checkoutFolder, "채번 작업을 위한 체크아웃");
	        return (EPMDocument) checkoutLink.getWorkingCopy();
	    }
	    
	    /**
	     * EPMDocument 체크인
	     */
	    private void checkinEPMDocument(EPMDocument epm) throws Exception {
	        WorkInProgressHelper.service.checkin(epm, "채번 작업 완료");
	    }
	    
	    /**
	     * EPMDocument 생명주기 상태 설정
	     */
	    private void setEPMDocumentLifeCycleState(EPMDocument epm, String stateName) throws Exception {
	        EPMDocument refreshedEPM = (EPMDocument) PersistenceHelper.manager.refresh(epm);
	        LifeCycleHelper.service.setLifeCycleState(refreshedEPM, State.toState(stateName));
	    }
	    
	    /**
	     * 3D 문서 처리
	     */
	    private void process3DDocument(EPMDocument epm3d, NumberingData data) throws Exception {
	        updateEPMDocumentIdentity(epm3d, data);
	        
	        String fileName = createCADFileName(epm3d, data);
	        updateCADFileName(epm3d, fileName);
	    }
	    
	    /**
	     * 2D 문서 처리
	     */
	    private void process2DDocument(EPMDocument epm2d, NumberingData baseData) throws Exception {
	        String drawingNumber = baseData.number + ".drw";
	        String drawingName = adjustNameLength2(baseData.name, MAX_NAME_LENGTH) + ".drw";
	        NumberingData drawingData = new NumberingData(drawingNumber, drawingName);
	        System.out.println("KKKKKKKKKK ::: "  + drawingData);
	        updateEPMDocumentIdentity(epm2d, drawingData);
	        
	        String fileName = createCADFileName(epm2d, drawingData);
	        
	        updateCADFileName(epm2d, drawingName);
	    }
	    
	    /**
	     * EPMDocument Identity 업데이트
	     */
	    private void updateEPMDocumentIdentity(EPMDocument epm, NumberingData data) throws Exception {
	    	System.out.println("OOOOOOOOO "  + data);
	        EPMDocumentMaster master = (EPMDocumentMaster) epm.getMaster();
	        EPMDocumentMasterIdentity identity = (EPMDocumentMasterIdentity) master.getIdentificationObject();
	        
	        identity.setNumber(data.number);
	        identity.setName(data.name);
	        
	        IdentityHelper.service.changeIdentity(master, identity);
	    }
	    
	    /**
	     * CAD 파일명 생성
	     */
	    private String createCADFileName(EPMDocument epm, NumberingData data) {
	        String docType = epm.getDocType().getStringValue();
	        String extension = getFileExtensionByDocType(docType);
	        
	        String baseName = adjustNameLength(data.name, MAX_NAME_LENGTH);
	        String fileName = baseName + "." + extension;
	        
	        return adjustNameLength(fileName, MAX_FIELD_LENGTH);
	    }
	    
	    /**
	     * 문서 타입별 파일 확장자 반환
	     */
	    private String getFileExtensionByDocType(String docType) {
	        switch (docType) {
	            case "wt.epm.EPMDocumentType.CADCOMPONENT":
	                return "prt";
	            case "wt.epm.EPMDocumentType.CADASSEMBLY":
	                return "asm";
	            case "wt.epm.EPMDocumentType.CADDRAWING":
	                return "drw";
	            default:
	                return "dat";
	        }
	    }
	    
	    /**
	     * CAD 파일명 업데이트
	     */
	    private void updateCADFileName(EPMDocument epm, String fileName) throws Exception {
	        WTKeyedMap docCadNameMap = new WTKeyedHashMap();
	        docCadNameMap.put(epm.getMaster(), strOutput(fileName));
	        EPMDocumentHelper.service.changeCADName(docCadNameMap);
	    }
	    
	    /**
	     * 승인 요청 데이터 정리
	     */
	    private void cleanupApprovalRequestData(SerialApprovalContext context) throws Exception {
	        try {
	            PersistenceHelper.manager.delete(context.serialListMapping);
	            PersistenceHelper.manager.delete(context.serialList);
	            PersistenceHelper.manager.delete(context.partSerialList);
	            
	        } catch (Exception e) {
	            System.err.println("승인 요청 데이터 삭제 실패: " + e.getMessage());
	            throw new Exception("승인 요청 데이터 삭제 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * 처리 상태 업데이트
	     */
	    private void updateProcessingStatus(SerialApprovalContext context, boolean isSuccess, 
	                                      String errorMessage, WTCollection processedItems) throws Exception {
	        
	        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	        
	        // PartSerialList 상태 업데이트
	        updatePartSerialListStatus(context, isSuccess, errorMessage, timestamp);
	        processedItems.add(context.partSerialList);
	        
	        // SerialList 상태 업데이트
	        if (context.serialList != null) {
	            context.serialList.setH2_REQ_STATUS("승인완료");
	            processedItems.add(context.serialList);
	        }
	        
	        PersistenceHelper.manager.modify(processedItems);
	    }
	    
	    /**
	     * PartSerialList 상태 업데이트
	     */
	    private void updatePartSerialListStatus(SerialApprovalContext context, boolean isSuccess, 
	                                          String errorMessage, Timestamp timestamp) throws Exception {
	        
	        try {
	            // 기본 상태 정보 설정
	            context.partSerialList.setH2_TARGET_STATUS("채번완료");
	            context.partSerialList.setH2_TARGET_REGISTER_DATE(timestamp);
	            context.partSerialList.setH2_SERIAL_STATE("Y");
	            
	            // 결과 상태 설정
	            String resultCheck = determineResultStatus(context, isSuccess);
	            context.partSerialList.setH2_IF_SUCCESS_DIV("S".equalsIgnoreCase(resultCheck) ? "Success" : "Error");
	            
	            // 메시지 설정
	            String message = determineResultMessage(context, isSuccess, errorMessage);
	            context.partSerialList.setH2_IF_MESSAGE("message: " + message);
	            
	            // Part Master OID 설정
	            String partMasterOID = extractPartMasterOID(context.part);
	            context.partSerialList.setH2_WTPART_MST_OID(partMasterOID);
	            
	        } catch (Exception e) {
	            System.err.println("PartSerialList 상태 업데이트 실패: " + e.getMessage());
	            e.printStackTrace();
	            
	            // 최소한의 상태라도 설정 시도
	            try {
	                context.partSerialList.setH2_TARGET_STATUS("처리오류");
	                context.partSerialList.setH2_SERIAL_STATE("N");
	                context.partSerialList.setH2_IF_SUCCESS_DIV("Error");
	                context.partSerialList.setH2_IF_MESSAGE("message: 상태 업데이트 중 오류 발생 - " + e.getMessage());
	            } catch (Exception fallbackEx) {
	                System.err.println("PartSerialList 최소 상태 설정도 실패: " + fallbackEx.getMessage());
	            }
	            
	            throw new Exception("PartSerialList 상태 업데이트 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * Part Master OID 추출 (안전한 방식)
	     */
	    private String extractPartMasterOID(WTPart part) throws Exception {
	        try {
	            if (part == null) {
	                throw new IllegalArgumentException("Part 객체가 null입니다.");
	            }
	            
	            if (part.getPersistInfo() == null) {
	                throw new IllegalStateException("Part의 PersistInfo가 null입니다.");
	            }
	            
	            if (part.getPersistInfo().getObjectIdentifier() == null) {
	                throw new IllegalStateException("Part의 ObjectIdentifier가 null입니다.");
	            }
	            
	            String fullOID = part.getPersistInfo().getObjectIdentifier().getStringValue();
	            if (fullOID == null || fullOID.trim().isEmpty()) {
	                throw new IllegalStateException("Part의 OID 문자열이 비어있습니다.");
	            }
	            
	            String[] oidParts = fullOID.split(":");
	            if (oidParts.length < 2) {
	                throw new IllegalStateException("Part OID 형식이 올바르지 않습니다: " + fullOID);
	            }
	            
	            return oidParts[1];
	            
	        } catch (Exception e) {
	            System.err.println("Part Master OID 추출 실패: " + e.getMessage());
	            throw new Exception("Part Master OID 추출 실패: " + e.getMessage(), e);
	        }
	    }
	    
	    /**
	     * 결과 상태 결정
	     */
	    private String determineResultStatus(SerialApprovalContext context, boolean isSuccess) {
	        if (!isSuccess) {
	            return "E";
	        }
	        
	        if (context.matchedResult != null) {
	            return Objects.toString(context.matchedResult.get("RESULT"), "S").trim();
	        }
	        
	        return "S";
	    }
	    
	    /**
	     * 결과 메시지 결정
	     */
	    private String determineResultMessage(SerialApprovalContext context, boolean isSuccess, String errorMessage) {
	        if (!isSuccess && !errorMessage.isEmpty()) {
	            return errorMessage;
	        }
	        
	        if (context.matchedResult != null) {
	            String message = Objects.toString(context.matchedResult.get("MESSAGE"), "").trim();
	            if (!message.isEmpty()) {
	                return message;
	            }
	        }
	        
	        return "MD Interface: MD → PLM 전달된 메시지(ET_RESULT:MESSAGE) 없음";
	    }
	    
	    /**
	     * 항목 처리 오류 핸들링
	     */
	    private void handleItemProcessingError(Map<String, Object> data, Exception e) throws Exception {
	        System.err.println("=== 항목 처리 오류 ===");
	        System.err.println("데이터: " + data);
	        System.err.println("오류: " + e.getMessage());
	        
	        // 필요시 개별 항목 오류에 대한 추가 처리 로직
	        // 예: 오류 로그 저장, 알림 발송 등
	    }
	    
	    // ========== 유틸리티 메서드들 ==========
	    
	    /**
	     * MATNR 추출
	     */
	    private String extractMATNR(Map<String, Object> matchedResult) {
	        Long matnrLong = (Long) matchedResult.get("MATNR");
	        return matnrLong.toString();
	    }
	    
	    /**
	     * 이름 길이 조정
	     */
	    private String adjustNameLength(String name, int maxLength) {
	        if (name == null || name.trim().isEmpty()) {
	            return DEFAULT_NAME;
	        }
	        
	        String cleanName = name.trim().replace(" ", "_").replace(":", "_");
	        return cleanName.length() > maxLength ? cleanName.substring(0, maxLength) : cleanName;
	    }
	    
	    private String adjustNameLength2(String name, int maxLength) {
	        if (name == null || name.trim().isEmpty()) {
	            return DEFAULT_NAME;
	        }
	        
	        String cleanName = name.trim().replace("_", ":");
	        return cleanName.length() > maxLength ? cleanName.substring(0, maxLength) : cleanName;
	    }
	    
	    
	    // ========== 내부 클래스들 ==========
	    
	    /**
	     * 채번 승인 컨텍스트 클래스
	     * 처리에 필요한 모든 데이터와 객체를 관리
	     */
	    private class SerialApprovalContext {
	        // 입력 데이터
	        private final String outClass;
	        private final String partOID;
	        private final String epmDocumentOID;
	        private final String partSerialListOID;
	        private final String serialListOID;
	        private final String serialListMappingOID;
	        
	        // Windchill 객체들
	        public WTPart part;
	        public EPMDocument epmDocument;
	        public WTPartSerialList partSerialList;
	        public SerialList serialList;
	        public SerialListMapping serialListMapping;
	        
	        // 처리 결과
	        public Map<String, Object> matchedResult;
	        
	        public SerialApprovalContext(Map<String, Object> data) {
	            this.outClass = Objects.toString(data.get("Class"), "");
	            this.partOID = Objects.toString(data.get("partOID"), "");
	            this.epmDocumentOID = Objects.toString(data.get("epmDocumentOID"), "");
	            this.partSerialListOID = Objects.toString(data.get("partSerialListOID"), "");
	            this.serialListOID = Objects.toString(data.get("serialListOID"), "");
	            this.serialListMappingOID = Objects.toString(data.get("serialListMappingOID"), "");
	        }
	        
	        /**
	         * Windchill 객체들 로드
	         */
	        public void loadWindchillObjects() throws Exception {
	            this.part = (WTPart) getClasszz(partOID);
	            this.epmDocument = (EPMDocument) getClasszz(epmDocumentOID);
	            this.partSerialList = (WTPartSerialList) getClasszz(partSerialListOID);
	            this.serialList = (SerialList) getClasszz(serialListOID);
	            this.serialListMapping = (SerialListMapping) getClasszz(serialListMappingOID);
	        }
	        
	        /**
	         * 매칭되는 결과 찾기
	         */
	        public void findMatchedResult(Object result) throws Exception {
	            Long targetId = part.getMaster().getPersistInfo().getObjectIdentifier().getId();
	            
	            if (result instanceof List) {
	                List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
	                this.matchedResult = resultList.stream()
	                    .filter(item -> targetId.equals(item.get("ZZPLM_ID")))
	                    .findFirst()
	                    .orElse(null);
	            } else if (result instanceof Map) {
	                Map<String, Object> resultMap = (Map<String, Object>) result;
	                if (targetId.equals(resultMap.get("ZZPLM_ID"))) {
	                    this.matchedResult = resultMap;
	                }
	            }
	            
	            if (this.matchedResult == null) {
	                throw new Exception("채번 결과 정보가 없습니다. Target ID: " + targetId);
	            }
	        }
	        
	        /**
	         * EPMDocument 존재 여부 확인
	         */
	        public boolean hasEPMDocument() {
	            return this.epmDocument != null;
	        }
	        
	        /**
	         * Part 번호 반환 (로깅용)
	         */
	        public String getPartNumber() {
	            return this.part != null ? this.part.getNumber() : "Unknown";
	        }
	    }
	    
	    /**
	     * 채번 데이터 클래스
	     */
	    private static class NumberingData {
	        public final String number;
	        public final String name;
	        
	        public NumberingData(String number, String name) {
	            this.number = number;
	            this.name = name;
	        }
	        
	        @Override
	        public String toString() {
	            return "NumberingData{number='" + number + "', name='" + name + "'}";
	        }
	    }
	    
	//
	//
	//------------------------------------------------------------------------------------------------------------------------

}


