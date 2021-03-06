package org.openmrs.module.xreports.page.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.reporting.ReportingConstants;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.xreports.NameValue;
import org.openmrs.module.xreports.XReport;
import org.openmrs.module.xreports.XReportGroup;
import org.openmrs.module.xreports.XReportsConstants;
import org.openmrs.module.xreports.api.XReportsService;
import org.openmrs.module.xreports.web.ReportCommandObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

public class RunReportsPageController {

	public String controller(PageModel model,
			@RequestParam(required = false, value = "groupId") Integer groupId,
			@RequestParam(required = false, value = "reportId") Integer reportId,
			@RequestParam(required = false, value = "reportTitle") String reportTitle,
			HttpSession session, HttpServletRequest request, HttpServletResponse response,
			UiSessionContext emrContext, UiUtils ui) throws Exception {

		emrContext.requireAuthentication();
		
		session.setAttribute(ReportingConstants.OPENMRS_REPORT_DATA, null);
		
		String patientId = request.getParameter("patientId");
		if (StringUtils.isNotBlank(patientId)) {
			String rptId = request.getParameter("reportId");
			if (StringUtils.isBlank(rptId)) {
				rptId = Context.getAdministrationService().getGlobalProperty("xreports.patientSummary.reportId");
			}
			if (StringUtils.isNotBlank(rptId)) {
				if (Context.getService(XReportsService.class).getReport(Integer.parseInt(rptId)) != null) {
					ReportCommandObject command = new ReportCommandObject();
					command.setCohort(new Cohort(patientId));
					request.getSession().setAttribute(XReportsConstants.REPORT_PARAMETER_DATA, command);
					return "redirect:/moduleServlet/xreports/exportPdfServlet?patientId=" + patientId + "&reportId=" + rptId;
				}
			}
		}
		
		if (reportId != null) {
			XReport report = Context.getService(XReportsService.class).getReport(reportId);
			if (report != null) {
				String uuid = report.getExternalReportUuid();
				if (StringUtils.isNotBlank(uuid)) {
					ReportDefinitionService rds = Context.getService(ReportDefinitionService.class);
					ReportDefinition reportDef = rds.getDefinitionByUuid(uuid);
					if (reportDef != null && reportDef.getParameters().size() > 0) {
						return "redirect:/xreports/reportParameter.page?reportId=" + reportId
						        + (groupId != null ? "&groupId=" + groupId : "");
					}
				}
			}
			return "redirect:/xreports/reportRunner.page?reportId=" + reportId + (groupId != null ? "&groupId=" + groupId : "");
		}
		
		List<XReport> reports = Context.getService(XReportsService.class).getReports(groupId);
		List<XReportGroup> groups = Context.getService(XReportsService.class).getReportGroups(groupId);
		
		model.addAttribute("reports", reports);
		model.addAttribute("groups", groups);
		
		List<NameValue> crumbs = new ArrayList<NameValue>();
		while (groupId != null) {
			XReportGroup group = Context.getService(XReportsService.class).getReportGroup(groupId);
			crumbs.add(0, new NameValue(group.getName(), group.getId().toString()));
			XReportGroup parent = group.getParentGroup();
			if (parent != null)
				groupId = parent.getGroupId();
			else
				groupId = null;
		}
		
		model.addAttribute("crumbs", crumbs);
		
		return null;
	}
}
