<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="wt.httpgw.URLFactory,
                   java.util.ResourceBundle,
                   com.ptc.netmarkets.util.utilResource,
                   com.ptc.netmarkets.model.*,
                   com.ptc.netmarkets.util.misc.*,
                   com.ptc.netmarkets.util.beans.NmSessionBean,
                   org.apache.logging.log4j.Logger,
                   wt.log4j.LogR,
                   wt.util.HTMLEncoder"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"              prefix="c"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/fmt"        prefix="fmt"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/core"       prefix="wc"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/components" prefix="jca"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/mvc"        prefix="mvc"%>
<%@ taglib tagdir="/WEB-INF/tags"                               prefix="wctags" %>
<%-- <%@include file="/WEB-INF/tnsplm/jsp/common/script_include.jsp"%>
<%@include file="/WEB-INF/tnsplm/jsp/common/css_include.jsp"%> --%>
<head>
<c:if test="${datas ne 'popup'}">
	<%@include file="/WEB-INF/tnsplm/jsp/common/script_include.jsp"%>
	<%@include file="/WEB-INF/tnsplm/jsp/common/css_include.jsp"%>
</c:if>
<%@include file="/WEB-INF/tnsplm/jsp/common/loading.jsp"%>
</head>
<body>
 <style>

 /* Layout Switcher 스타일 */
        .layout-switcher {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 12px 16px;
            background: #f8f9fa;
            border-radius: 8px;
            margin-bottom: 10px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            justify-content: flex-end;
            margin-left: auto;
            width: fit-content;
        }

        .layout-switcher-label {
            font-size: 14px;
            font-weight: 600;
            color: #495057;
            margin-right: 12px;
        }

        .layout-switcher-buttons {
            display: flex;
            background: #ffffff;
            border-radius: 6px;
            border: 1px solid #dee2e6;
            overflow: hidden;
        }

        .layout-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 40px;
            height: 40px;
            border: none;
            background: transparent;
            cursor: pointer;
            transition: all 0.2s ease;
            position: relative;
        }

        .layout-btn:hover {
            background: #e9ecef;
        }

        .layout-btn.active {
            background: #007bff;
            color: white;
        }

        .layout-btn.active:hover {
            background: #0056b3;
        }

        .layout-btn + .layout-btn {
            border-left: 1px solid #dee2e6;
        }

        .layout-btn.active + .layout-btn {
            border-left-color: #007bff;
        }

        /* 아이콘 스타일 */
        .icon-grid, .icon-list {
            width: 18px;
            height: 18px;
            fill: currentColor;
        }

        /* IBSheet 그리드 컨테이너 */
        #ib-container1 {
            transition: all 0.3s ease;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            overflow: hidden;
        }

        /* 카드 뷰 컨테이너 */
        .card-view {
            padding: 20px;
            /* background: #f8f9fa; */
            width: 100%;
            box-sizing: border-box;
            overflow-x: hidden;
        }

        /* 그룹 헤더 */
        .group-header {
            background: linear-gradient(135deg, #007bff, #0056b3);
            color: white;
            padding: 16px 20px;
            border-radius: 12px 12px 0 0;
            margin: 30px 0 0 0;
            font-weight: 700;
            font-size: 16px;
            box-shadow: 0 2px 8px rgba(0,123,255,0.2);
            position: relative;
            width: 100%;
            box-sizing: border-box;
        }

        .group-header:first-child {
            margin-top: 0;
        }

        .group-header::before {
            content: '';
            position: absolute;
            top: -10px;
            left: 20px;
            width: 0;
            height: 0;
            border-left: 10px solid transparent;
            border-right: 10px solid transparent;
            border-bottom: 10px solid #007bff;
        }

        .group-count {
            background: rgba(255,255,255,0.2);
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 14px;
            margin-left: 12px;
        }

        /* 그룹 컨테이너 */
        .group-container {
            background: white;
            border-radius: 0 0 12px 12px;
            padding: 20px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            margin-bottom: 20px;
            width: 100%;
            box-sizing: border-box;
        }
        
        .group-container.collapsed {
		    max-height: 0;
		    padding: 0 20px;
		    margin-bottom: 0;
		    opacity: 0;
		}

        .group-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            width: 100%;
        }

        /* 카드 아이템 스타일 */
        .card-item {
            background: white;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.08);
            transition: all 0.2s ease;
            border-left: 4px solid #e9ecef;
            position: relative;
            width: 100%;
            box-sizing: border-box;
            max-width: 100%;
            word-wrap: break-word;
            overflow-wrap: break-word;
        }

        .card-item:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
        }

        .card-item.selected {
            border-left-color: #007bff;
            background: #f8f9ff;
        }

        /* 카드 헤더 */
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 16px;
        }

        .card-checkbox {
            width: 18px;
            height: 18px;
            margin-right: 0;
        }

        .card-number {
            background: #e9ecef;
            color: #495057;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
        }

        /* 카드 타이틀 */
        .card-title {
            font-size: 18px;
            font-weight: 700;
            color: #212529;
            margin-bottom: 8px;
            line-height: 1.3;
        }

        .card-subtitle {
            font-size: 14px;
            color: #6c757d;
            margin-bottom: 16px;
        }

        /* 카드 정보 그리드 */
        .card-info {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            margin-bottom: 16px;
        }

        .card-info-item {
            display: flex;
            flex-direction: column;
        }

        .card-info-label {
            font-size: 11px;
            color: #868e96;
            font-weight: 600;
            text-transform: uppercase;
            margin-bottom: 4px;
        }

        .card-info-value {
            font-size: 13px;
            color: #495057;
            font-weight: 500;
        }

        /* 상태 표시 */
        .card-status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            margin-right: 10px;
            font-size: 12px;
            font-weight: 600;
            text-align: center;
        }

        .status-progress { background: #fff3cd; color: #856404; }
        .status-complete { background: #d4edda; color: #155724; }
        .status-pending { background: #f8d7da; color: #721c24; }
        .status-review { background: #d1ecf1; color: #0c5460; }

        /* 카드 푸터 */
        .card-footer {
            display: flex;
            justify-content: between;
            align-items: center;
            margin-top: 16px;
            padding-top: 16px;
            border-top: 1px solid #e9ecef;
        }

        .card-actions {
            display: flex;
            gap: 8px;
        }

        .card-action-btn {
            padding: 6px 12px;
            border: 1px solid #dee2e6;
            background: white;
            border-radius: 4px;
            font-size: 12px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .card-action-btn:hover {
            background: #f8f9fa;
            border-color: #adb5bd;
        }

        .card-verification {
            font-size: 12px;
            padding: 4px 8px;
            border-radius: 4px;
            font-weight: 600;
        }

        .verification-success { background: #d4edda; color: #155724; }
        .verification-error { background: #f8d7da; color: #721c24; }

        .item-title {
            font-size: 18px;
            font-weight: 600;
            color: #212529;
            margin-bottom: 8px;
        }

        .item-description {
            font-size: 14px;
            color: #6c757d;
            line-height: 1.5;
        }

        .item-meta {
            font-size: 12px;
            color: #adb5bd;
            margin-top: 8px;
        }

        /* 반응형 디자인 */
        @media (max-width: 1200px) {
            .group-cards {
                grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                gap: 16px;
            }
        }
        
        @media (max-width: 768px) {
            .layout-switcher {
                padding: 8px 12px;
            }
            
            .layout-switcher-label {
                font-size: 13px;
            }
            
            .layout-btn {
                width: 36px;
                height: 36px;
            }
            
            .card-view {
                padding: 16px;
            }
            
            .group-cards {
                grid-template-columns: 1fr;
                gap: 12px;
            }
            
            .card-item {
                padding: 16px;
            }
            
            .group-container {
                padding: 16px;
            }
        }

        @media (max-width: 480px) {
            .card-view {
                padding: 12px;
            }
            
            .group-cards {
                gap: 8px;
            }
            
            .card-item {
                padding: 12px;
            }
        }
 
</style>
	
	<div class="customContainer" style="border:0px;">
	<div class="searchContainer">
		<table class="view TableIsBorder" style="margin-bottom:0px;">
			<tbody class="mainTbody TableIsBorder">
				<tr class="mainTr TableIsBorder">
					<th class="mainTh mainThBlue TableIsBorder" style="width:75%;"><span class="sa_span4 black">대상 승인요청 상태</span></th>
					<td class="mainTd" style="width:100%;">
						<select class="mainSelect" id="JsonTypeCode1">
							<option>전체</option>
							<option>임시저장</option>
							<!-- <option>회수됨</option>
							<option>진행중</option>
							<option>반려됨</option> -->
							<option>승인완료</option>
						</select>
					</td>
					<th class="mainTh mainThBlue TableIsBorder" style="width:75%;"><span class="sa_span4 black">채번 품목 상태</span></th>
					<td class="mainTd" style="width:100%;">
						<select class="mainSelect" id="JsonTypeCode2">
							<option>전체</option>
<!-- 							<option>승인요청대상</option>
							<option>채번대상</option>
							<option>대상지정됨</option>
							<option>승인중</option>-->
 							<option>채번완료</option>
						</select>
					</td>
					<!-- </tr>
					<tr class="mainTr TableIsBorder"> -->
					<!-- <th class="mainTh mainThBlue TableIsBorder" style="width:75%;"><span class="sa_span4 white">작업자</span></th>
					<td class="mainTd" style="width:100%;">
						<input class="mainInput" id="searchUser" type="text" />
					</td> -->
					<!-- <th class="mainTh mainThBlue TableIsBorder" style="width:75%;"><span class="sa_span4 white">대상 작업일</span></th>
					<td class="mainTd" style="width:100%;">
						<input class="mainInput mainDate" id="dateStart" type="date" /> -
						<input class="mainInput mainDate" id="dateEnd" type="date" />
					</td> -->
					<div class="page-header">
						<div class="searchButtonWrapper">
							<c:choose>
								<c:when test="${datas ne 'popup'}">
									<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="조회" style="" onclick="searchList();" />
								</c:when>
								<c:otherwise>
									<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" type="button" value="조회" style="" onclick="searchList1();" />
									<input type="hidden" class="ui-icon-closethick"/>
								</c:otherwise>
							</c:choose>
						</div>
					</div>
				</tr>
			</tbody>
		</table>
	</div>
	</div>
	<div class="">
	<c:choose>
		<c:when test="${datas ne 'popup'}">
		<div class="searchButtonWrapper">
		
			<!-- 		<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="유사자재 조회" 
			id="AnalogySelect" data-value="유사자재 조회"  data-url="/serial/action/analogySelect"
			onclick="actionButton(this);" /> -->
			
						<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="추가(CAD)" 
			id="createCAD" data-value="추가(CAD)" data-url="/serial/action/cad/cad" 
			onclick="actionButton(this);"/>
			
						<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="행 추가(Non-CAD)" 
			id="createNonCAD" data-value="행 추가(Non-CAD)" data-url="/serial/action/row"  data-sheet="mySheet1"
			onclick="actionButton(this);"/>
			
						<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="일괄입력" 
			id="ALLAddData" data-value="일괄입력"  data-url="/serial/action/actionAllAdd"
			onclick="actionButton(this);" />
			
						<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="채번요청" 
			id="MATNumberApproval" data-value="채번요청" data-url="/serial/action/actionSerial" data-action="0" 
			onclick="actionButton(this);"  />
			
						<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="수정" 
			id="actionEdit" data-value="수정" data-url="/serial/action/actionEdit" 
			onclick="actionButton(this);"/>
			
			<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton" type="button" value="삭제"  onclick="delData();" />
			
			<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only endButton"  id="excelDownload" name="excelDownload" type="button" value="Excel" />
			
			<%-- 결재요청에 한화면으로 퉁 --%>
			<!-- <input class="customSelectBtn mainSearchButton endButton" type="button" value="승인요청" 
			id="RFApproval" data-value="승인요청"  data-url="/serial/action/actionApproval"
			onclick="actionButton(this);" /> -->
			
			<%-- 실 자재 등록 NON_CAD 호출 --%>
			<%-- <input class="customSelectBtn mainSearchButton endButton" type="button" value="등록(Non-CAD)[삭제예정]" 
			id="createNonCAD" data-value="등록(Non-CAD)" data-url="/serial/action/cad/not_cad"  
			onclick="actionButton(this);"/> --%>
		</div>
		</c:when>
		<c:otherwise>
		<div class="page-header">
		<div class="searchButtonWrapper">
			<input class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"  type="button" value="추가"
			data-sheet ="mySheet4"
			onclick="popAction(this);" />
			<input type="button"  style="display:none;" data-i18n="common.btn.close" class="endButton"/ >
		</div>
		</div>
		</c:otherwise>
	</c:choose>
		
		
		
		<!-- <a href="#" data-popup="true" data-type="part" data-action="0" data-oid="" onclick="navAction(this);">(테스트용) 부품 탭 메뉴</a>
        <a href="#" data-popup="true" data-type="epm"  data-action="0" data-oid="" onclick="navAction(this);">(테스트용) CAD 탭 메뉴</a>
        <a href="#" data-popup="false" data-type="doc"  data-action="0" data-oid="" onclick="navAction(this);">(테스트용) 문서 탭 메뉴</a>
        <input type="button" onclick="javascript:aaa();" value="javaScript로 파일 다운" />
        <a href="/Windchill/tnsplm/serial/test?fileName=C:\Users\Administrator\Documents\pim_installmgr.log" >A태그 다운로드</a> -->
	</div>
	<br/>
	
		<c:choose>
			<c:when test="${datas ne 'popup'}">
				<!-- ibsheet 테이블 -->
			<div class="layout-switcher">
				<div class="layout-switcher-buttons">
					<button class="layout-btn active" data-view="table" title="리스트 보기" > <!-- style="display: none;" -->
						<svg class="icon-list" viewBox="0 0 24 24">
		                  <path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z" />
                  		</svg>
					</button>
					<button class="layout-btn" data-view="card" title="카드 보기">
						<svg class="icon-list" viewBox="0 0 24 24">
                    		<path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" />
                		</svg>
					</button>
				</div>
			</div>
			<div id="ib-container1" ></div>
			<div id="card-container" class="card-view" style="display: none; overflow-y: auto; height:650px;"></div>
			</c:when>
			<c:otherwise>
				<div id="ib-container1-popup" ></div>
			</c:otherwise>
		</c:choose>

<c:choose>
<c:when test="${datas ne 'popup'}">

<script>
        // Layout Switcher 기능 (데이터 변수 기반)
        const layoutButtons = document.querySelectorAll('.layout-btn');
        const ibContainer = document.getElementById('ib-container1');
        const cardContainer = document.getElementById('card-container');
        let currentViewType = 'table';
        let currentData = []; // 현재 데이터를 저장할 변수

        layoutButtons.forEach(button => {
            button.addEventListener('click', function() {
                // 모든 버튼에서 active 클래스 제거
                layoutButtons.forEach(btn => btn.classList.remove('active'));
                
                // 클릭된 버튼에 active 클래스 추가
                this.classList.add('active');
                
                // 뷰 타입 가져오기
                const viewType = this.getAttribute('data-view');
                currentViewType = viewType;
                
                if (viewType === 'table') {
                    // IBSheet 테이블 표시
                    ibContainer.style.display = 'block';
                    cardContainer.style.display = 'none';
                } else if (viewType === 'card') {
                    // 카드 뷰 표시
                    //console.log('card')
                    ibContainer.style.display = 'none';
                    cardContainer.style.display = 'grid';
                    
                    // 저장된 데이터로 카드 뷰 생성
                    buildCardViewFromData();
                }
            });
        });

        // 데이터 업데이트 함수 (JSP에서 호출)
        function updateLayoutData(data) {
        	//console.log('updateLayoutData: ',data);
            currentData = data || [];
            if (currentViewType === 'card') {
                buildCardViewFromData();
            }
        }

        // 데이터로부터 카드 뷰 생성
       function buildCardViewFromData() {
            //console.log('buildCardViewFromData 호출됨');
            
            if (!currentData || currentData.length === 0) {
                cardContainer.innerHTML = '<div style="text-align: center; padding: 40px; color: #6c757d;">데이터가 없습니다.</div>';
                return;
            }

            var groupedData = {};
            
            // 데이터를 ApprovalNumber별로 그룹화
            for (var i = 0; i < currentData.length; i++) {
                var rowData = currentData[i];
                var approvalNumber = rowData.ApprovalNumber || '미분류';
                
                if (!groupedData[approvalNumber]) {
                    groupedData[approvalNumber] = [];
                }
                rowData.rowIndex = i + 1;
                groupedData[approvalNumber].push(rowData);
            }

            var allHtml = "";
            
            // 그룹별로 카드 HTML 생성 (당신이 원하는 방식)
            for (var approvalNumber in groupedData) {
                var groupItems = groupedData[approvalNumber];
                var groupTitle = approvalNumber === '미분류' ? '승인요청번호 없음' : '승인요청번호: ' + approvalNumber;
                
                // 그룹 헤더 (당신이 원하는 방식으로!)
                allHtml = allHtml + '<div class="group-header" id="' + approvalNumber + '" onclick="toggleGroup(\'' + approvalNumber + '\')">';
                allHtml = allHtml + groupTitle;
                allHtml = allHtml + '<span class="group-count">' + groupItems.length + '건</span>';
                allHtml = allHtml + '<span class="group-toggle-icon"></span>';
                allHtml = allHtml + '</div>';
                allHtml = allHtml + '<div class="group-container" data-group="' + approvalNumber + '"><div class="group-cards">';

                // 그룹 내 카드들
                for (var j = 0; j < groupItems.length; j++) {
                    var rowData = groupItems[j];
                    
                    // 상태에 따른 CSS 클래스 결정
                    var statusClass = 'status-pending';
                    var status = rowData.H2_REQ_STATUS || '';
                    if (status.indexOf('완료') > -1 || status.indexOf('승인') > -1 || rowData.H2_TARGET_STATUS === '채번완료') {
                        statusClass = 'status-complete';
                    } else if (status.indexOf('진행') > -1 || status.indexOf('처리') > -1) {
                        statusClass = 'status-progress';
                    } else if (status.indexOf('검토') > -1 || status.indexOf('대기') > -1) {
                        statusClass = 'status-review';
                    }

                    // 검증 상태
                    var verificationClass = 'verification-success';
                    var verificationText = rowData.Success || '대기';
                    if (verificationText === 'Error' || verificationText.indexOf('실패') > -1 || verificationText.indexOf('오류') > -1) {
                        verificationClass = 'verification-error';
                    }

                    // 파일 확장자에 따른 아이콘 표시
                    var fileName = rowData.FileName || '';
                    var fileIcon = '';
                    if (fileName.indexOf('.prt') > -1) fileIcon = '📦';
                    else if (fileName.indexOf('.asm') > -1) fileIcon = '🔧';
                    else if (fileName.indexOf('.drw') > -1) fileIcon = '📋';
                    else if (fileName) fileIcon = '📄';

                    // 카드 HTML 생성 (완전한 단순 문자열 연결)
                    var cardItem = "";
                    cardItem = cardItem + '<div class="card-item';
                    if (rowData.sCheckBox) cardItem = cardItem + ' selected';
                    cardItem = cardItem + '" data-row="';
                    cardItem = cardItem + rowData.rowIndex;
                    cardItem = cardItem + '">';
                    
                    // 카드 헤더
                    cardItem = cardItem + '<div class="card-header">';
                    cardItem = cardItem + '<input type="checkbox" class="card-checkbox"';
                    if (rowData.sCheckBox) cardItem = cardItem + ' checked';
                    cardItem = cardItem + ' onchange="toggleCardSelection(';
                    cardItem = cardItem + rowData.rowIndex;
                    cardItem = cardItem + ', this.checked)">';
                    cardItem = cardItem + '<span class="card-number">';
                    cardItem = cardItem + (rowData.No || rowData.rowIndex);
                    cardItem = cardItem + '</span>';
                    cardItem = cardItem + '</div>';
                    
                    // 카드 타이틀
                    cardItem = cardItem + '<div class="card-title">';
                    cardItem = cardItem + (rowData.name || '제품명 없음');
                    cardItem = cardItem + '</div>';
                    cardItem = cardItem + '<div class="card-subtitle">';
                    cardItem = cardItem + fileIcon;
                    cardItem = cardItem + ' ';
                    cardItem = cardItem + (fileName || '파일명 없음');
                    cardItem = cardItem + '</div>';
                    
                    // 카드 정보
                    cardItem = cardItem + '<div class="card-info">';
                    cardItem = cardItem + '<div class="card-info-item"><span class="card-info-label">품번</span><span class="card-info-value">';
                    cardItem = cardItem + (rowData.number || '-');
                    cardItem = cardItem + '</span></div>';
                    cardItem = cardItem + '<div class="card-info-item"><span class="card-info-label">버전</span><span class="card-info-value">';
                    cardItem = cardItem + (rowData.ver || '-');
                    cardItem = cardItem + '</span></div>';
                    cardItem = cardItem + '<div class="card-info-item"><span class="card-info-label">분류체계</span><span class="card-info-value">';
                    cardItem = cardItem + (rowData.ClassName || '-');
                    cardItem = cardItem + '</span></div>';
                    cardItem = cardItem + '<div class="card-info-item"><span class="card-info-label">작성자</span><span class="card-info-value">';
                    cardItem = cardItem + (rowData.H2_REG_USER || '-');
                    cardItem = cardItem + '</span></div>';
                    
                    // 선택적 필드들
                    if (rowData.MATERIAL) {
                        cardItem = cardItem + '<div class="card-info-item"><span class="card-info-label">재질</span><span class="card-info-value">';
                        cardItem = cardItem + rowData.MATERIAL;
                        cardItem = cardItem + '</span></div>';
                    }
                    if (rowData.IS3S === 'X') {
                        cardItem = cardItem + '<div class="card-info-item"><span class="card-info-label">IS3S</span><span class="card-info-value">✅ 적용</span></div>';
                    }
                    cardItem = cardItem + '</div>';
                    
                    // 카드 푸터
                    cardItem = cardItem + '<div class="card-footer">';
                    cardItem = cardItem + '<div class="card-status ';
                    cardItem = cardItem + statusClass;
                    cardItem = cardItem + '">';
                    if (rowData.H2_TARGET_STATUS === '채번완료') {
                        cardItem = cardItem + '채번완료';
                    } else {
                        cardItem = cardItem + (rowData.H2_REQ_STATUS || '대기');
                    }
                    cardItem = cardItem + '</div>';
                    cardItem = cardItem + '<div class="card-verification ';
                    cardItem = cardItem + verificationClass;
                    cardItem = cardItem + '">';
                    cardItem = cardItem + verificationText;
                    cardItem = cardItem + '</div>';
                    cardItem = cardItem + '</div>';
                    
                    // 메시지
                    if (rowData.Message) {
                        var borderColor = '#28a745';
                        if (verificationClass === 'verification-error') {
                            borderColor = '#dc3545';
                        }
                        cardItem = cardItem + '<div style="margin-top: 12px; font-size: 12px; color: #6c757d; padding: 8px; background: #f8f9fa; border-radius: 4px; border-left: 3px solid ';
                        cardItem = cardItem + borderColor;
                        cardItem = cardItem + ';">';
                        cardItem = cardItem + rowData.Message.replace('message: ', '');
                        cardItem = cardItem + '</div>';
                    }
                    
                    cardItem = cardItem + '</div>';
                    allHtml = allHtml + cardItem;
                }
                
                // 그룹 컨테이너 닫기
                allHtml = allHtml + '</div></div>';
            }

            cardContainer.innerHTML = allHtml;
        }
        
        

        // 카드 선택/해제 함수
        function toggleCardSelection(row, checked) {
            if (typeof mySheet1 !== 'undefined') {
                mySheet1.SetCellValue(row, 'sCheckBox', checked ? 1 : 0);
                
                // 카드 아이템 선택 표시 토글
                const cardItem = document.querySelector(`[data-row="${row}"]`);
                if (cardItem) {
                    if (checked) {
                        cardItem.classList.add('selected');
                    } else {
                        cardItem.classList.remove('selected');
                    }
                }
                
                // 기존 IBSheet 이벤트 호출
                if (typeof mySheet1_OnChange === 'function') {
                    const rowData = mySheet1.GetRowData(row);
                    mySheet1_OnChange(row, 'sCheckBox', checked);
                }
            }
        }

        // 그룹 토글 함수
       function toggleGroup(approvalNumber) {
            var groupHeader = document.getElementById(approvalNumber);
            var groupContainer = document.querySelector('[data-group="' + approvalNumber + '"]');
            var toggleIcon = groupHeader.querySelector('.group-toggle-icon');
            
            if (groupContainer && groupHeader && toggleIcon) {
                // 클래스 토글
                groupContainer.classList.toggle('collapsed');
                groupHeader.classList.toggle('collapsed');
                
                // 아이콘 변경
                if (groupContainer.classList.contains('collapsed')) {
                    //toggleIcon.textContent = '▶';
                } else {
                    //toggleIcon.textContent = '▼';
                }
            }
        }


        // JSP에서 사용할 수 있는 함수들
        function switchToTableView() {
            document.querySelector('[data-view="table"]').click();
            addGroupToggleButton()
        }

        function switchToCardView() {
            document.querySelector('[data-view="card"]').click();
        }

        function getCurrentView() {
            return currentViewType;
        }

        function refreshCardView() {
            if (currentViewType === 'card') {
                convertToCardView();
            }
        }

        // IBSheet 그룹 기능 토글
        let isGrouped = false; // 그룹 상태를 직접 관리
        
        function toggleTableGrouping() {
            if (typeof mySheet1 === 'undefined') return;
            
            if (isGrouped) {
                // 그룹 해제
                mySheet1.HideGroupRow();
                isGrouped = false;
            } else {
                // ApprovalNumber로 그룹화
                //mySheet1.ShowGroupRow('ApprovalNumber', true);
                // 그룹 표시 포맷 설정
                mySheet1.ShowGroupRow('ApprovalNumber', '승인요청번호: {%s} <font color="gray">({%c}건)</font>');
                isGrouped = true;
            }
        }

        // 그룹화 버튼 추가 함수 (선택사항)
        function addGroupToggleButton() {
            const layoutSwitcher = document.querySelector('.layout-switcher');
            if (!layoutSwitcher.querySelector('.group-toggle-btn')) {
                const groupButton = document.createElement('button');
                groupButton.className = 'group-toggle-btn';
                groupButton.innerHTML = '📊 그룹화';
                groupButton.title = '승인요청번호별 그룹화';
                groupButton.style.cssText = `
                    padding: 8px 16px;
                    margin-left: 12px;
                	margin-right: 10px;
                    background: #28a745;
                    color: white;
                    border: none;
                    height: 40px;
                    border-radius: 6px;
                    font-size: 14px;
                    cursor: pointer;
                    transition: background 0.2s ease;
                `;
                
                groupButton.addEventListener('click', function() {
                    if (currentViewType === 'table') {
                        toggleTableGrouping();
                        this.style.background = isGrouped ? '#dc3545' : '#28a745';
                        this.innerHTML = isGrouped ? '📊 그룹해제' : '📊 그룹화';
                    }
                });
                
                groupButton.addEventListener('mouseenter', function() {
                    this.style.background = currentViewType === 'table' ? 
                        (isGrouped ? '#c82333' : '#218838') : '#6c757d';
                });
                
                groupButton.addEventListener('mouseleave', function() {
                    this.style.background = currentViewType === 'table' ? 
                        (isGrouped ? '#dc3545' : '#28a745') : '#6c757d';
                });
                
                layoutSwitcher.appendChild(groupButton);
            }
        }

        // IBSheet 데이터 로드 후 카드 뷰 갱신을 위한 전역 함수
        window.refreshLayoutSwitcher = function() {
            if (currentViewType === 'card') {
                setTimeout(convertToCardView, 100); // IBSheet 로딩 완료 후 실행
            }
        };

        // 페이지 로드 시 초기 설정
        document.addEventListener('DOMContentLoaded', function() {
            // 기본값으로 테이블 뷰 설정
            switchToTableView();
        });
    </script>
  
<script>
	function LoadPage() {
	  var MainGridInfo = {};
	  MainGridInfo.Cfg = {ExportMode: 2, SearchMode:0, SelectionRowsMode:1, ToolTip:1, DeferredVScroll:1, OnSort:1, CheckActionMode:1, MouseHoverMode: 0, AutoFitColWidth: "resize"}; //SizeMode:sizeNoVScroll, 
	  MainGridInfo.HeaderMode = {Sort:1, ColMove:1, ColResize:1, HeaderCheck:1};
	  MainGridInfo.Cols = [
		  /* {Header : "No2",  Type:"Text",Width:40,SaveName:"Rows.ibidx",Align:"Center"}, */
		  {Header : "",  Type:"Text",Width:40,SaveName:"partOID",Align:"Center", Hidden: 1, Edit:false},
		  {Header : "",  Type:"Text",Width:40,SaveName:"epmDocumentOID",Align:"Center", Hidden: 1, Edit:false},
		  {Header : "",  Type:"Text",Width:40,SaveName:"partSerialListOID",Align:"Center", Hidden: 1, Edit:false},
		  {Header : "",  Type:"Text",Width:40,SaveName:"serialListOID",Align:"Center", Hidden: 1, Edit:false},
		  {Header : "",  Type:"Text",Width:40,SaveName:"serialListMappingOID",Align:"Center", Hidden: 1, Edit:false},
		  {Header : "", Type:"CheckBox",Width:30,SaveName:"sCheckBox",Align:"Center"},
		  {Header : "No",  Type:"Seq",Width:40,SaveName:"No",Align:"Center"},
		  {Header : "",  Type:"Text",Width:40,SaveName:"CHECK_REQ",Align:"Center", Hidden: 1, Edit:false},
  		  {Header : "파일명", Type:"Text",Width:70,SaveName:"FileName",Align:"Center", Edit:false},
		  {Header : "품명", Type:"Text",Width:100,SaveName:"name",Align:"Center", Edit:false, FontUnderline:1, FontColor:"blue" },
		  {Header : "품번", Type:"Text",Width:100,SaveName:"number",Align:"Center", Edit:false },
		  {Header : "VER", Type:"Text",Width:50,SaveName:"ver",Align:"Center", Edit:false},
		  {Header : "승인요청번호", Type:"Text",Width:100,SaveName:"ApprovalNumber",Align:"Center", Edit:false, FontUnderline:1, FontColor:"blue" },
		  {Header : "진행상태", Type:"Text",Width:55,SaveName:"H2_REQ_STATUS",Align:"Center", Edit:false},
		  //{Header : "썸네일", Type:"Text",Width:45,SaveName:"Thumbnail",Align:"Center", Edit:false},
		  //{Header : "BOM", Type:"Text",Width:50,SaveName:"BOM",Align:"Center", Edit:false},
		  {Header : "채번품목상태", Type:"Text",Width:100,SaveName:"H2_TARGET_STATUS",Align:"Center", Edit:false},
		  //{Header : "TITLE", Type:"Text",Width:80,SaveName:"TITLE",Align:"Center", Edit:false},
		  {Header : "분류체계", Type:"Text",Width:70,SaveName:"ClassName",Align:"Center", Edit:false},
	 	  {Header : "분류체계n", Type:"Text",Width:70,SaveName:"Class",Align:"Center",Hidden: 1, Edit:false},
	 	 {Header : "IS3S",  Type:"Text",Width:40,SaveName:"IS3S",Align:"Center", Edit:false},
		  //{Header : "Herarchy", Type:"Text",Width:100,SaveName:"HIERARCHY",Align:"Center", Edit:false},
		  //{Header : "형상기호", Type:"Text",Width:100,SaveName:"HR",Align:"Center", Edit:false},
		  //{Header : "재질", Type:"Text",Width:100,SaveName:"MATERIAL",Align:"Center", Edit:false},
		  //{Header : "표면처리", Type:"Text",Width:100,SaveName:"PC",Align:"Center", Edit:false},
		  /* {Header : "작성자", Type:"Text",Width:100,SaveName:"H2_REG_USER",Align:"Center", Edit:false},
		  {Header : "검토자", Type:"Text",Width:100,SaveName:"Reviewer",Align:"Center", Edit:false},
		  {Header : "승인자", Type:"Text",Width:100,SaveName:"ApprovedPerson",Align:"Center", Edit:false}, */
		  {Header : "검증", Type:"Text",Width:100,SaveName:"Success",Align:"Center", Edit:false},
		  {Header : "메시지", Type:"Text",Width:100,SaveName:"Message",Align:"Center", Edit:false},
		  {Header : "",  Type:"Text",Width:40,SaveName:"Color",Align:"Center", Hidden: 1, Edit:false},
	  ]; 
	  
	  var container = document.getElementById("ib-container1");
      createIBSheet2(container, "mySheet1", "100%", "600px");
	  mySheet1.SetEditable(true);
	  // 초기화 필수
	  IBS_InitSheet(mySheet1, MainGridInfo);
	  
	  // 이벤트
	  //if (this.setIBEvents) {
	  //}

	  // 조회 조건 필터
	  doAction();
	}
	
   function doAction() {
	    // 그룹기능
	    //mySheet1.ShowGroupRow('', '{%s} <font color="gray">({%c}건)</font>');
	    // 조회 조건 필터 활성화
	    mySheet1.ShowFilterRow();
	    // 조회 조건 필터 숨김
	  	mySheet1.HideFilterRow();
	  	mySheet1.FitColWidth();
	  	// 트리
	  	mySheet1.SetTreeCheckActionMode(1);
	    mySheet1.SetHeaderRowHeight(40);
	    mySheet1.LoadSearchData(this.data, {
	        Sync: 1
	    });
	    
   }
   $( '#excelDownload' ).click( function(){
		mySheet1.Down2Excel({
               'FileName': 'SERIAL.xlsx',
               'SheetName': 'Sheet',
               'Mode': -1
           });
	});
   
   //----------------------------------------------------------------------
   // [선택] 그리드 내 데이터 선택
   //
   var serialListDatas = {};
   function mySheet1_OnChange(row, cell, checked){
	   var data = mySheet1.GetRowData(row);
	   if(checked == true){
		   serialListDatas[data.No] = data;
	   }else {
		   delete serialListDatas[data.No];
	   }
	   console.log(serialListDatas)
   }
   
   //----------------------------------------------------------------------
   // [검색] 검색조건 조회
   //
   async function searchList(){
		var searchMaster = document.querySelector('.customContainer');
		var searchData = searchMaster.querySelectorAll('div select, div input');
		var formData = {};
		searchData.forEach(function(item){
			formData[item.id === "" ? "null" : item.id] = item.value;
		});	
		const data = {"data" : await responseCall("/serial/getList", formData)};
		mySheet1.LoadSearchData(data, {Sync:1});
		
		if (data && data.data) {
	        updateLayoutData(data.data);
	    }
		
		serialListDatas = {};
	}
   
	document.addEventListener("DOMContentLoaded", function() {
		LoadPage();
	});
	
   //----------------------------------------------------------------------
   // [Text] 색상 변경
   //
	function mySheet1_OnRowSearchEnd(row){
		var data  = mySheet1.GetRowData(row);
		//console.log(data.Color)
		if(data.Color ==="Y"){
			mySheet1.SetRowBackColor(row, '#F05650');
		}
	}
	
   //----------------------------------------------------------------------
   // [Text] 마우스 호버
   //
	function mySheet1_OnMouseMove(Button, Shift, X, Y) { 
	    var row = mySheet1.MouseRow();   
	    var col = mySheet1.MouseCol();

	    if (col == 12 || col == 9) { 
	        if (mySheet1.GetCellValue(row, col) == "") {
	            mySheet1.SetDataLinkMouse(col, 0);    
	        } else {
	            mySheet1.SetDataLinkMouse(col, 1);
	        }
	    }
	}

   //----------------------------------------------------------------------
   // [데이터 삭제] 그리드 내 데이터 선택 데이터 삭제
   //
   async function delData(){
	   let check = document.getElementById('popupId');
	   if (check) {
	       if (check.value !== "") {
	           return;
	       }
	   }
	   
	   if (Object.keys(serialListDatas).length === 0) { 
		    alert("삭제할 데이터가 없습니다."); 
		    return;
	   }
	   const confirmSave = confirm("삭제하시겠습니까?");
	   if(confirmSave){
       //----------------------------------------------
	   // [Server] 선택 데이터 서버 통신
	   //
	   const data = await responseCall("/serial/action/del/serial", serialListDatas);
	   if(!!data){
		   alert(data);
		   searchList();
	   }else{
		   return;
	   }
	   //----------------------------------------------
	   // [Client] 그리드 리스트 내 데이터 삭제
	   //
	   Object.keys(serialListDatas).forEach(e => {
			   var targetRow = sheet1FindRowByNo(e);
				if(targetRow !== -1){
					mySheet1.RowDelete(targetRow);
				}
	   });
	   //----------------------------------------------
	   //변수 초기화
	   //
	   serialListDatas = {};
	   }else{
		   alert("삭제가 취소 되었습니다.");
	   }
   }
   
   //----------------------------------------------------------------------
   // [버튼 팝업] 레이아웃 팝업
   //
   var abClass = {
		   get :[]
   }
   async function actionButton(item){
	   var isCall = true;
	   /* item.preventDefault(); */
	   var mainurl = "/Windchill/tnsplm/serial/getButtonPopUp";
	   var url = item.dataset.url;
	   let check = document.getElementById('popupId');
	   if (check) {
	       if (check.value !== "") {
	           return;
	       }
	   }
	   if(item.dataset.value === "수정"){
		   if(Object.keys(serialListDatas).length === 0){
			   alert("선택된 항목이 없습니다.");
			   return;
		   }else if(Object.keys(serialListDatas).length !== 1){
			   alert("하나의 정보만 수정 가능합니다.");
			   return;
		   }
		   var editSTATUS = false;
		   Object.entries(serialListDatas).forEach(([key, item]) => {
			  if(item.H2_TARGET_STATUS === "채번완료"){
				  alert("채번이 완료된 자재 입니다.");
				  editSTATUS = true;
			  } 
		   });
		   
		   if(editSTATUS){
			   return;
		   }
		   
	   }else if(item.dataset.value === "채번요청"){
		   if (Object.keys(serialListDatas).length === 0) {
		       alert("하나 이상의 자재를 선택해 결재 요청 하세요.");
		       return;
		   }
		   let oidCheck = '';
		   let classData = '';
		   let classNot = true;  // 이 값이 false로 바뀌면 종료돼야 함
		   let oidNot = true;    // 이 값이 false로 바뀌면 종료돼야 함
		   let compareClass = true; // 이 값이 false로 바뀌면 종료돼야 함
		   let oidCheckCount = 0;
		   let IS3S = '';

//		   console.log('채번요청', serialListDatas);
		   // 각 항목에 대해 검사
		   Object.entries(serialListDatas).forEach(([key, item]) => {
			   console.log(key, item.IS3S)
			   if(!item.partOID){
				   alert('자재가 없는 데이터가 선택되어 있습니다.');
				   classNot = false;
				   return;
			   }
		       // 분류체계가 없으면 경고하고 종료
		       if (!item.Class) {
		           alert('분류체계가 없는 자재가 선택되어 있습니다.');
		           classNot = false;
		           return;  // 종료
		       }
		       // 클래스 데이터 저장
		       if (!classData) {
		           classData = item.Class;
		           console.log(classData);
		       }
		       abClass.get.push({ ClassId: item.Class });
		       // 승인요청번호 처리
		       if (!oidCheck && item.serialListOID || !oidCheck) {
		           oidCheck = item.serialListOID;
		       } else if (oidCheck && item.serialListOID && oidCheck !== item.serialListOID) {
		           alert('승인요청번호가 다른 자재가 존재합니다.');
		           oidNot = false;
		           return;  // 종료
		       }
		       // 결재 미대상 자재가 결재 대상 자재와 함께 있을 경우 처리
		       if(item.IS3S != "X"){
		    	   IS3S = "N" //item.IS3S;
		       }

		       // 분류체계가 다르면 경고하고 종료
		       if (classData !== item.Class ) { // || oidCheckCount > 1
		           compareClass = false;
		           return;  // 종료
		       }
		   });

		   // TODO:  미결재 대상 결재 MD 통신 → 결재로 변경
		   if(IS3S == 'N'){
			   if (confirm("결재 미대상이 선택 되었습니다.\n결재 미대상에 대한 자재 채번 작업을 시작하시겠습니까?")) {
				   const list = Array.isArray(serialListDatas) ? serialListDatas : Object.values(serialListDatas);
				   console.log('new List: ', list);
				   const filterList = filterObject(list, {IS3S: ''});
				   console.log('new filterList:' , filterList);
				   var resultData = {
						   //isCheck : true,
	            		   isApproval : false,
	            		   OID : "",
	            		   List : [] 
            		};

				   resultData.List = filterList;
				   console.log('new resultData: ', resultData)
				   var result = await responseCall("/serial/if/ifsmd0007", resultData);
	               if(!!result){
	            	   alert(result);
	               }
	               searchList(); // 리프레쉬
			   }
			   abClass.get = [];
	           return;  // 종료
		   }
		  		   
		   if(!compareClass){
			   if(confirm("분류체계가 다른 정보가 선택되었습니다. \n그래도 채번을 진행 하시겠습니까?")){
					compareClass = true;
				}else{
					alert('취소 되었습니다.');
				}
		   }
		   if (!oidNot || !classNot || !compareClass ) { // 
			   isCall = false;
		       return;  // 여기서 실제 종료
		   }
	  } else if(item.dataset.value === "일괄입력"){
		   if(Object.keys(serialListDatas).length === 0){
			   alert("선택된 자재가 없습니다.");
			   isCall = false;
			   return;
		   }
	  }
 	  if(isCall){
 		 if(!!url){
		     arg = {
				  url: mainurl,
				  method: "POST",
				  title: item.dataset.value,
				  options:{
					  width: "50%",
					  height: "800",
					  //modal: true // 이거 때문에 팝업 필터가 동작 안함
				  },
				  data : {
						path: url	,
						datas:serialListDatas
				  }
		  }
		$.dialogPopup(arg, function(response) { 
			alert (response);
		});
	   }else{
		   return;
	   }
	  }
   }
   
   function filterObject(obj, filters) {
		let rtnData = obj.filter(item => 
			Object.keys(filters)
			.every(key => item[key] === filters[key]));
		
		return rtnData;
	}
 
   //----------------------------------------------------------------------
   // [페이지 팝업] 리스트 내 브라우저 팝업 상세페이지
   //
   function mySheet1_OnSelectCell(OldRow, OldCol, NewRow, NewCol, isDelete){
	   var data = mySheet1.GetRowData(NewRow);
	   var item = {
			dataset: {
					popup: "false",
					action: "0"
			}	   
	   };
	   
	   switch(NewCol){
	   	  case 9 :
	   			mySheet1_OnChange(NewRow, NewCol, true);
	   			document.getElementById('actionEdit').onclick();
	   			mySheet1_OnChange(NewRow, NewCol, false);
	   			break;
	   	  case 12 :
		   		  if(!!data.serialListOID){	   		
		   			mySheet1_OnChange(NewRow, NewCol, true);
		   			document.getElementById('MATNumberApproval').onclick();
		   			mySheet1_OnChange(NewRow, NewCol, false);
		   		  }
	   		  break;
			default: 
	   }
   }
   function sheet1FindRowByNo(noValue) {
	    var rowCount = mySheet4.RowCount();
	    for (var i = 1; i <= rowCount; i++) {
	        var cellValue = mySheet4.GetCellValue(i, "No");
	        if (cellValue == noValue) {
	            return i; // 찾은 row index
	        }
	    }
	    return -1; // 못 찾음
	}
   
   document.addEventListener("keydown", function(event) {
	   if (event.key === "F5") {
	   event.preventDefault();
	   location.reload();
	   }
	   });
	    

   
</script>

</c:when>
<c:otherwise>

<script>
function popLoadPage() {
  var popMainGridInfo = {};
  popMainGridInfo.Cfg = {SearchMode:0, SelectionRowsMode:1, ToolTip:1, DeferredVScroll:1, OnSort:1, CheckActionMode:1, MouseHoverMode: 0, AutoFitColWidth: "resize"}; //SizeMode:sizeNoVScroll, 
  popMainGridInfo.HeaderMode = {Sort:1, ColMove:1, ColResize:1, HeaderCheck:1};
  popMainGridInfo.Cols = [
	  /* {Header : "No2",  Type:"Text",Width:40,SaveName:"Rows.ibidx",Align:"Center"}, */
	  {Header : "",  Type:"Text",Width:40,SaveName:"partOID",Align:"Center", Hidden: 1, Edit:false},
	  {Header : "",  Type:"Text",Width:40,SaveName:"epmDocumentOID",Align:"Center", Hidden: 1, Edit:false},
	  {Header : "",  Type:"Text",Width:40,SaveName:"partSerialListOID",Align:"Center", Hidden: 1, Edit:false},
	  {Header : "",  Type:"Text",Width:40,SaveName:"serialListOID",Align:"Center", Hidden: 1, Edit:false},
	  {Header : "",  Type:"Text",Width:40,SaveName:"serialListMappingOID",Align:"Center", Hidden: 1, Edit:false},
	  {Header : "", Type:"CheckBox",Width:30,SaveName:"sCheckBox",Align:"Center"},
	  {Header : "No",  Type:"Seq",Width:40,SaveName:"No",Align:"Center"},
	  {Header : "",  Type:"Text",Width:40,SaveName:"CHECK_REQ",Align:"Center", Hidden: 1, Edit:false},
		  {Header : "파일명", Type:"Text",Width:70,SaveName:"FileName",Align:"Center", Edit:false },
	  {Header : "품명", Type:"Text",Width:100,SaveName:"name",Align:"Center", Edit:false },
	  {Header : "품번", Type:"Text",Width:100,SaveName:"number",Align:"Center", Edit:false },
	  {Header : "VER", Type:"Text",Width:50,SaveName:"ver",Align:"Center", Edit:false},
	  {Header : "승인요청번호", Type:"Text",Width:100,SaveName:"ApprovalNumber",Align:"Center", Edit:false },
	  {Header : "진행상태", Type:"Text",Width:55,SaveName:"H2_REQ_STATUS",Align:"Center", Edit:false},
	  //{Header : "썸네일", Type:"Text",Width:45,SaveName:"Thumbnail",Align:"Center", Edit:false},
	  //{Header : "BOM", Type:"Text",Width:50,SaveName:"BOM",Align:"Center", Edit:false},
	  {Header : "채번품목상태", Type:"Text",Width:100,SaveName:"H2_TARGET_STATUS",Align:"Center", Edit:false},
	  //{Header : "TITLE", Type:"Text",Width:80,SaveName:"TITLE",Align:"Center", Edit:false},
	  {Header : "분류체계", Type:"Text",Width:70,SaveName:"ClassName",Align:"Center", Edit:false},
 	  {Header : "분류체계n", Type:"Text",Width:70,SaveName:"Class",Align:"Center",Hidden: 1, Edit:false},
 	 {Header : "IS3S",  Type:"Text",Width:40,SaveName:"IS3S",Align:"Center", Edit:false},
	  //{Header : "Herarchy", Type:"Text",Width:100,SaveName:"HIERARCHY",Align:"Center", Edit:false},
	  //{Header : "형상기호", Type:"Text",Width:100,SaveName:"HR",Align:"Center", Edit:false},
	  //{Header : "재질", Type:"Text",Width:100,SaveName:"MATERIAL",Align:"Center", Edit:false},
	  //{Header : "표면처리", Type:"Text",Width:100,SaveName:"PC",Align:"Center", Edit:false},
	  /* {Header : "작성자", Type:"Text",Width:100,SaveName:"H2_REG_USER",Align:"Center", Edit:false},
	  {Header : "검토자", Type:"Text",Width:100,SaveName:"Reviewer",Align:"Center", Edit:false},
	  {Header : "승인자", Type:"Text",Width:100,SaveName:"ApprovedPerson",Align:"Center", Edit:false}, */
	  {Header : "검증", Type:"Text",Width:100,SaveName:"Success",Align:"Center", Edit:false},
	  {Header : "메시지", Type:"Text",Width:100,SaveName:"Message",Align:"Center", Edit:false},
	  {Header : "",  Type:"Text",Width:40,SaveName:"Color",Align:"Center", Hidden: 1, Edit:false},
  ]; 
  
  var popContainer = document.getElementById("ib-container1-popup");
  createIBSheet2(popContainer, "popMySheet1", "100%", "358px");
  popMySheet1.SetEditable(true);
  // 초기화 필수
  IBS_InitSheet(popMySheet1, popMainGridInfo);

  // 조회 조건 필터
  doAction1();
  
}
    

function doAction1() {
    // 그룹기능
    //popMySheet1.ShowGroupRow('', '{%s} <font color="gray">({%c}건)</font>');
    // 조회 조건 필터 활성화
    popMySheet1.ShowFilterRow();
    // 조회 조건 필터 숨김
  	popMySheet1.HideFilterRow();
  	popMySheet1.FitColWidth();
  	// 트리
  	popMySheet1.SetTreeCheckActionMode(1);
    popMySheet1.SetHeaderRowHeight(40);
    popMySheet1.LoadSearchData(this.data, {
        Sync: 1
    });
}

//----------------------------------------------------------------------
// [선택] 그리드 내 데이터 선택
//
var popserialListDatas = {};
function popMySheet1_OnChange(row, cell, checked){
   
   var data = popMySheet1.GetRowData(row);
   console.log(data)
   if(checked == true){
	   popserialListDatas[data.No] = data;
   }else {
	   delete popserialListDatas[data.No];
   }
   
}
//----------------------------------------------------------------------
// [검색] 검색조건 조회
//
async function searchList1(){
	var searchMaster = document.querySelector('.customContainer');
	var searchData = searchMaster.querySelectorAll('div select, div input');
	var formData = {};
	searchData.forEach(function(item){
		formData[item.id === "" ? "null" : item.id] = item.value;
	});	
	const data = {"data" : await responseCall("/serial/getList", formData)};
	popMySheet1.LoadSearchData(data);
	
	popserialListDatas = {};
}

//----------------------------------------------------------------------
// [Text] 마우스 호버
//
//function popMySheet1_OnMouseMove(Button, Shift, X, Y) { }

//----------------------------------------------------------------------
// [Text] 색상 변경
//
function popMySheet1_OnRowSearchEnd(row){
	var data  = popMySheet1.GetRowData(row);
	if(data.Color ==="Y"){
		popMySheet1.SetRowBackColor(row, '#F05650');
	}
}

//----------------------------------------------------------------------
// [페이지 팝업] 리스트 내 브라우저 팝업 상세페이지
//
// function popMySheet1_OnSelectCell(OldRow, OldCol, NewRow, NewCol, isDelete){ }

function popAction(item){
   if(Object.keys(popserialListDatas).length === 0){
	   alert('하나 이상 자재를 선택해주세요');
   }
   const list = Array.isArray(popserialListDatas) ? popserialListDatas : Object.values(popserialListDatas);
   list.forEach(e => {
		// FindText
		var checkGrid = mySheet4.FindText("partOID", e.partOID);
		if(checkGrid != "-1"){
			alert('이미 요청리스트에 존재하는 데이터 입니다.\n해당 자재는 선택에서 제외 됩니다.');
			return;
		}
		if(e.Class == ""){
			alert('분류체계가 존재하지 않는 자재가 존재 합니다.\n해당 자재는 선택에서 제외 됩니다.');
			return;
		}
		if (e.serialListOID || e.serialListOID.trim() !== "") {
			alert('이미 자재['+ e.name +']는 승인번호가 부여된 상태입니다.\n해당 자재는 선택에서 제외 됩니다.');
			return;
		}
		if(e.IS3S == "X"){
			alert('IS3S에 해당하는 ['+ e.name +']는 현재 화면에서 추가가 불가능 합니다.');
			return;
		}

		// 새 행 삽입
		const lastRow = mySheet4.RowCount() + 1;
		mySheet4.DataInsert(lastRow);

		// 그리드 셀 값 삽입
		mySheet4.SetCellValue(lastRow, "partOID", e.partOID);
		mySheet4.SetCellValue(lastRow, "epmDocumentOID", e.epmDocumentOID);
		mySheet4.SetCellValue(lastRow, "partSerialListOID", e.partSerialListOID);
		mySheet4.SetCellValue(lastRow, "serialListOID", e.serialListOID);
		mySheet4.SetCellValue(lastRow, "serialListMappingOID", e.serialListMappingOID);
		mySheet4.SetCellValue(lastRow, "ApprovalNumber", e.ApprovalNumber || "");
		mySheet4.SetCellValue(lastRow, "H2_REQ_STATUS", e.H2_REQ_STATUS || "");
		mySheet4.SetCellValue(lastRow, "Thumbnail", e.Thumbnail || "");
		mySheet4.SetCellValue(lastRow, "FileName", e.FileName || "");
		mySheet4.SetCellValue(lastRow, "name", e.name || "");
		mySheet4.SetCellValue(lastRow, "number", e.number || "");
		mySheet4.SetCellValue(lastRow, "H2_TARGET_STATUS", e.H2_TARGET_STATUS || "");
		mySheet4.SetCellValue(lastRow, "TITLE", e.TITLE || "");
		mySheet4.SetCellValue(lastRow, "Class", e.Class || "");
		mySheet4.SetCellValue(lastRow, "HIERARCHY", e.HIERARCHY || "");
		mySheet4.SetCellValue(lastRow, "HR", e.HR || "");
		mySheet4.SetCellValue(lastRow, "MATERIAL", e.MATERIAL || "");
		mySheet4.SetCellValue(lastRow, "PC", e.PC || "");
		mySheet4.SetCellValue(lastRow, "H2_REG_USER", e.H2_REG_USER || "");
		mySheet4.SetCellValue(lastRow, "Reviewer", e.Reviewer || "");
		mySheet4.SetCellValue(lastRow, "ApprovedPerson", e.ApprovedPerson || "");
		// 조건에 따른 값 추가 처리
		if (!e.serialListOID || e.serialListOID.trim() === "") {
			mySheet4.SetCellValue(lastRow, "isSerialMissing", true);
			mySheet4.SetCellValue(lastRow, "serialNumber", getSerialNumber());
			mySheet4.SetCellValue(lastRow, "serialListOID", getActionSerialSLOID());
		}
		
		// 신규 추가 색상 추가
		mySheet4.SetRowBackColor(lastRow, '#F05650');
	});
   
   //----------------------------------------------
   // [Client] 그리드 리스트 내 데이터 삭제
   //
  Object.keys(popserialListDatas).forEach(e => {
		   var targetRow = popSheet1FindRowByNo(e);
			if(targetRow !== -1){
				popMySheet1.RowDelete(targetRow);
			}
   });
   
   //----------------------------------------------
   //변수 초기화
   //
   popserialListDatas = {};
}

//----------------------------------------------------------------------
//[데이터 삭제] 그리드 내 데이터 선택 데이터 삭제
//
function popSheet1FindRowByNo(noValue) {
  var rowCount = popMySheet1.RowCount();
  for (var i = 1; i <= rowCount; i++) {
      var cellValue = popMySheet1.GetCellValue(i, "No");
      if (cellValue == noValue) {
          return i; // 찾은 row index
      }
  }
  return -1; // 못 찾음
}

//----------------------------------------------------------------------
//[실행] 스크립트 실행
//
$(document).ready(function() {
	try {
		popMySheet1.DisposeSheet();
	} catch (e) {
		console.log("dispose error", e);	
	}

	popLoadPage();
	
});

//팝업 화면에 닫기 버튼  
$("#${popupId}  .ui-icon-closethick").click(function(e){
	//alert()
	popMySheet1.DisposeSheet();
	$("#${popupId}").dialog("close");
	$("#${popupId}").remove();
});

function popsheet1FindRowByNo(noValue) {
    var rowCount = mySheet4.RowCount();
    for (var i = 1; i <= rowCount; i++) {
        var cellValue = mySheet4.GetCellValue(i, "No");
        if (cellValue == noValue) {
            return i; // 찾은 row index
        }
    }
    return -1; // 못 찾음
}
</script>
</c:otherwise>
</c:choose>
</body>
