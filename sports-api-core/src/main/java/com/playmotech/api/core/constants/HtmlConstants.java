package com.playmotech.api.core.constants;

public final class HtmlConstants {
	public final static String TABLE_ROW = "<tr> {{tableData}} <td class=\"rating\">{{rating}}</td></tr>";
	public final static String TABLE_CONTENT = "<div class=\"section box\">\n" + "        <h2> {{tableHeading}} </h2>\n"
			+ "        <table>\n" + "            <thead>\n" + "                {{tableHead}}\n"
			+ "            </thead>\n" + "            <tbody>\n" + "                {{tableBody}}      \n"
			+ "            </tbody>\n" + "        </table>\n" + "<div class=\"centered\">\n"
			+ "        <img src=\"{{donutChart}}\" alt=\"Donut Chart\"></img>\n" + "    </div>\n" + "    </div>";
	public final static String TABLE_HEAD = "<tr><th>Criteria</th><th>Benchmark</th><th>Remark</th><th>Rating</th></tr>";
	public final static String PSYCHOLOGICAL_TABLE_HEAD = "<tr><th>Task</th><th>Remarks</th><th>Rating</th></tr>";
}
