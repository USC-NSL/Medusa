<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<? 
/* PHP code section */ 
include_once "libphp/libutils.php"; 
include_once "libphp/libviewer.php"; 

?>

<head>
	<meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
	<title>MedAuthor</title>
	<link rel="stylesheet" type="text/css" href= "css/styles.css" />
	<script type="text/JavaScript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
	<script type="text/JavaScript" src="libjs/jquery.base64.js"></script>

	<script type="text/javascript">
		/* global variable */
		var medscript;

		function get_time() {
			var dTime = new Date();
			var hours = dTime.getHours();
			var minute = dTime.getMinutes();
			var sec = dTime.getSeconds();
			return hours + ":" + minute + ":" + sec;
		}

		function dlog(text) {
			$('div.log').append(get_time() + ' ' + text + '<br>');
			$('div.log').each( function() {
				var scrollHeight = Math.max(this.scrollHeight, this.clientHeight);
				this.scrollTop = scrollHeight - this.clientHeight;
			});
		}

		function clear_dlog() {
			$('div.log').html('');
		}

		function str_with_tag(tag, content) {
			if (content == '') {
				return '';
			}
			else {
				return "<" + tag + ">" + content + "</" + tag + ">\n";
			}
		}

		/* class definition */
		function Stage()
		{
			this.name = '';
			this.type = 'SPC';
			this.binary = '';
			this.inst = '';
			this.trigger = '';
			this.review = '';
			this.preview = '';
			/* under config tag */
			this.input = '';
			this.output = '';
			this.params = '';
			this.stmt = '';
			this.reward = '';
			this.expiration = '';

			this.set_default = function() {
				$('input[name=in_s_name]').val('[stage name]');
				$('select[name=in_s_type]').val('SPC');
				$('input[name=in_s_binary]').val('[medusalet name]');
				$('input[name=in_s_inst]').val('');
				$('select[name=in_s_trigger]').val('user-initiated');
				$('select[name=in_s_review]').val('none');
				$('select[name=in_s_preview]').val('user-initiated');
				$('input[name=in_s_input]').val('');
				$('input[name=in_s_output]').val('');
				$('input[name=in_s_params]').val('');
				$('input[name=in_s_stmt]').val('[Instruction for AMT Post]');
				$('input[name=in_s_reward]').val('0.01');
			}

			this.refresh = function() {
				$('input[name=in_s_name]').val(this.name);
				$('select[name=in_s_type]').val(this.type);
				$('input[name=in_s_binary]').val(this.binary);
				$('input[name=in_s_inst]').val(this.inst);
				$('select[name=in_s_trigger]').val(this.trigger);
				$('select[name=in_s_review]').val(this.review);
				$('select[name=in_s_preview]').val(this.preview);
				$('input[name=in_s_input]').val(this.input);
				$('input[name=in_s_output]').val(this.output);
				$('input[name=in_s_params]').val(this.params);
				$('input[name=in_s_stmt]').val(this.stmt);
				$('input[name=in_s_reward]').val(this.reward);
				$('input[name=in_s_expire]').val(this.expiration);
			}
		}

		function MedScript() 
		{
			this.name = '';
			this.cmdpush = '';
			this.rrid = '';
			this.rrkey = '';
			this.wwid = '';
			this.timeout = '';

			this.maxidx = 0;	/* 0: global info, 1, 2, 3, ... : stages */
			this.idx = 0;		/* 0: global info, 1, 2, 3, ... : stages */
			this.stages = [];

			this.get_fname = function() {
				return this.name + "_created_by_webtool.xml";
			}

			this.run = function() {
				var dbreq_uri = "insertmysql_ajax.php?opcode=insert&cmd=start&medscript=" + this.get_fname();
            	$.get(dbreq_uri, function() {
            	    dlog('* [' + medscript.name + '] app has started.<br>');
            	});
			}

			this.save_program = function(name) {
				var xml_desc = this.get_xml_desc();
				//dlog('* raw: ' + xml_desc);
				xml_desc = $.base64.encode(xml_desc);
				//dlog('* encoded: ' + xml_desc);

				var post_data = {};
				post_data['path'] = 'program/' + this.get_fname();
				post_data['content'] = xml_desc;
				$.post('save_medscript.php', post_data, function(data) {
					dlog(data);
					dlog('* the program has been saved successfully.');
				});
			}

			this.get_xml_desc = function() {
				var xmldesc = '';
				xmldesc = '<xml>\n<app>\n';
				// global info.
				xmldesc += str_with_tag('name', this.name);
				xmldesc += str_with_tag('cmdpush', this.cmdpush);
				xmldesc += str_with_tag('rrid', this.rrid);
				xmldesc += str_with_tag('rrkey', this.rrkey);
				xmldesc += str_with_tag('wwid', this.wwid);
				xmldesc += str_with_tag('timeout', this.timeout);

				// stage description.
				var stage_str;
				var i;

				for (i = 0; i < this.maxidx; i++) {
					stage_str = '';
					stage_str += str_with_tag('name', this.stages[i].name);
					stage_str += str_with_tag('type', this.stages[i].type);
					stage_str += str_with_tag('binary', this.stages[i].binary);
					stage_str += str_with_tag('inst', this.stages[i].inst);
					stage_str += str_with_tag('trigger', this.stages[i].trigger);
					stage_str += str_with_tag('review', this.stages[i].review);
					stage_str += str_with_tag('preview', this.stages[i].preview);
					stage_str += '<config>';
					stage_str += str_with_tag('input', this.stages[i].input);
					stage_str += str_with_tag('output', this.stages[i].output);
					if (this.stages[i].type == 'SPC') {
						stage_str += str_with_tag('params', this.stages[i].params);
					}
					else {
						stage_str += str_with_tag('stmt', this.stages[i].stmt);
						stage_str += str_with_tag('reward', this.stages[i].reward);
						stage_str += str_with_tag('expiration', this.stages[i].expiration);
					}
					stage_str += '</config>';

					xmldesc += str_with_tag('stage', stage_str);
				}

				xmldesc += '\n';

				// connector
				var conn_str;

				for (i = 1; i < this.maxidx; i++) {
					conn_str = '';
					conn_str += str_with_tag('src', this.stages[i-1].name);
					conn_str += str_with_tag('dst', '<success>' + this.stages[i].name + 
											'</success>' + str_with_tag('failure', this.stages[0].name));

					xmldesc += str_with_tag('connector', conn_str);
				}
			
				xmldesc += '</app>\n</xml>\n';

				//alert(xmldesc);
				return xmldesc;
			}

			this.save = function() {
				if (this.idx > 0) {
					var stage = this.get_cur_stage();

					stage.name = $('input[name=in_s_name]')[0].value;
					stage.type = $('select[name=in_s_type]')[0].value;
					stage.binary = $('input[name=in_s_binary]')[0].value;
					stage.inst = $('input[name=in_s_inst]')[0].value;
					stage.trigger = $('select[name=in_s_trigger]')[0].value;
					stage.review = $('select[name=in_s_review]')[0].value;
					stage.preview = $('select[name=in_s_preview]')[0].value;
					stage.input = $('input[name=in_s_input]')[0].value;
					stage.output = $('input[name=in_s_output]')[0].value;
					stage.params = $('input[name=in_s_params]')[0].value;
					stage.stmt = $('input[name=in_s_stmt]')[0].value;
					stage.reward = $('input[name=in_s_reward]')[0].value;
					stage.expiration = $('input[name=in_s_expire]')[0].value;

					dlog('* saved a stage configuration.');
					dlog(' -> stage_name: ' + stage.name +
								', stage_type: ' + stage.type +
								', stage_binary: ' + stage.binary +
								', stage_trigger: ' + stage.trigger +
								', stage_review: ' + stage.review +
								', stage_preview: ' + stage.preview +
								', stage_input: ' + stage.input +
								', stage_output: ' + stage.output);
				}
				else {
					this.name = $('input[name=in_g_appname]')[0].value;
					this.cmdpush = $('select[name=in_g_cmdpush]')[0].value;
					this.rrid = $('input[name=in_g_rrid]')[0].value;
					this.rrkey = $('input[name=in_g_rrkey]')[0].value;
					this.wwid = $('input[name=in_g_wwid]')[0].value;
					this.timeout = $('input[name=in_g_timeout]')[0].value;

					dlog('* saved an app configuration.');
					dlog(' -> name: ' + this.name+
								', cmdpush: ' + this.cmdpush+
								', rrid: ' + this.rrid +
								', rrkey: ' + this.rrkey +
								', wwid: ' + this.wwid +
								', timeout: ' + this.timeout);
				}
				this.update_control_flow_diagram();
			}

			this.move_idx = function(ofs) {
				// save current status first.
				this.idx = this.idx + ofs;
				if (this.idx < 0) this.idx = 0;
				if (this.idx > this.maxidx) {
					var st = new Stage();
					this.stages.push(st);
					this.maxidx = this.idx;
					st.set_default();

					dlog('* created a stage (idx=' + this.idx + ')');
				}
				else {
					// if not creating a new stage, 
					// should update the content using existing data.
					if (this.idx > 0) {
						this.stages[this.idx-1].refresh();
					}
				}

				//dlog('* current idx: ' + this.idx);

				// change UI components appropriately.
				if (this.idx == 0) {
					show_global_info(false);
				}
				else {
					show_stage_info();
				}
				this.update_control_flow_diagram();
			}
			
			this.get_cur_stage = function() {
				if (this.idx == 0) {
					return null;
				}
				else {
					return this.stages[this.idx-1];
				}
			}

			this.get_idx = function() {
				return this.idx;
			}

			this.update_control_flow_diagram = function() {
				var i = 0;
				var content = '';

				content += "<table height=100 style=\"margin-left:10px\">";
				if (this.name !== '')
					content += "<tr><td width=120px height=10px>* App Name: </td><td><b>" + this.name + "</b></td></tr>";
				if (this.cmdpush !== '') {
					content += "<tr><td height=10px>* Push Mechanism: </td><td><b>" + this.cmdpush + "</b></td></tr>";
					content += "<tr><td height=10px>* C2DM Client-ID: </td><td><b>" + this.wwid + "</b></td></tr>";
				}
				content += "<tr><td></td></tr></table>";
				// padding
				content += "<center><table height=40></table>";

				content += "<table>";
				content += "<tr>";
				for (i = 0; i <= this.maxidx-1; i++) {
					content += "<td></td><td><center>";
					if (i == this.idx-1) content += "*";
					content += "</td>";
				}
				content += "</tr><tr>";
				for (i = 0; i <= this.maxidx-1; i++) {
					content += "<td>";
					if (i > 0) {
						content += "<img src='img/conn.png' height=60>";
					}
					content += "</td><td><center>";

					if (this.stages[i].type == 'HIT') {
						content += "<img src='img/stage_hit.png' height=60>";
					}
					else if (this.stages[i].type == 'SPC') {
						content += "<img src='img/stage_spc.png' height=60>";
					}
					else {
						dlog('! unknown stage type: ' + this.stages[i].type);
					}
					content += "</center></td>";
				}
				content += "</tr><tr>";
				for (i = 0; i <= this.maxidx-1; i++) {
					content += "<td></td><td><center>" + this.stages[i].name + "</td>";
				}
				content += "</tr></table>";
				
				$('.control_flow_diagram').html(content);
			}
		}

		function change_form_visibility(elename, cmd) {
			if (cmd == 'show') {
				$('div').filter(elename).find('input').removeAttr("disabled").end().show();
				$('div').filter(elename).find('select').removeAttr("disabled").end().show();
			}
			else if (cmd == 'hide') {
				$('div').filter(elename).find('input').attr("disabled","disabled").end().hide();
				$('div').filter(elename).find('select').attr("disabled","disabled").end().hide();
			}
			else {
				alert('! unknown command: ' + cmd);
			}
		}

		function show_global_info(reset) { 
			if (reset == true) {
				medscript = new MedScript();
				clear_dlog();
				dlog('* start new program.');
			}

			change_form_visibility('.global_info', 'show');
			change_form_visibility('.conn_info', 'hide');
			change_form_visibility('.stage_info', 'hide');

			medscript.update_control_flow_diagram();
		}

		function show_stage_info() {
			change_form_visibility('.global_info', 'hide');
			change_form_visibility('.conn_info', 'hide');
			change_form_visibility('.stage_info', 'show');

			var type = $('select[name=in_s_type]')[0].value;
			if (type == 'SPC') {
				change_form_visibility('.stage_info_spc_config', 'show');
				change_form_visibility('.stage_info_hit_config', 'hide');
			}
			else {
				change_form_visibility('.stage_info_spc_config', 'hide');
				change_form_visibility('.stage_info_hit_config', 'show');
			}
		}

		function add_new_connector() {
			change_form_visibility('.stage_info', 'hide');
			change_form_visibility('.conn_info', 'show');
		}

		/* jquery routine */
		(function($) {
			$(document).ready(function() {

				show_global_info(true);

				$('#ajaxStatus')
					.ajaxStart(function() {
						$(this).show();
					})
					.ajaxStop(function() {
						$(this).hide();
					});

				$('select[name=in_s_type]').change(function() {
					//alert('Value change to ' + $(this).attr('value'));
					show_stage_info();
				});

				$('#exec_program').click(function() {
					if (medscript.name == '[app name]') {
						alert('! please specify the app name');
					}
					else {
						dlog('* execute medscript program [' + medscript.name + ']');
						medscript.run();
					}
				});

				$('#save_program').click(function() {
					if (medscript.name == '[app name]') {
						alert('! please specify the app name');
					}
					else {
						dlog('* save medscript program [' + medscript.name + ']');
						medscript.save_program();
					}
				});

				$('#prev').click(function() {
					medscript.move_idx(-1);
				});

				$('#save').click(function() {
					medscript.save();
				});

				$('#next').click(function() {
					medscript.move_idx(1);
				});

			});
		})(jQuery);

		function clear_field(obj) { obj.value = ''; }
		function reload_page() { location.replace("./"); }

	</script>
</head>

<body>
	<div id="container">

	<table style="background: white">
	<tr>
		<td width=700 height=60px>
			<center><h1>MedAuthor</h1></center>
		</td>
		<td width=200 style="background: white">
			<center>
			<form>
				<input type="button" value="Start new program" onclick="show_global_info(true)">
			</form>
		</td>
	</tr>
	</table>


	<!-- Tables -->
	<table border=1>
		<tr>
			<!--<td width=250><center><b>Existing Medusalets</b></center></td>-->
			<td width=340 style="background: #F5BEb3"><center><b>Configuration</b></center></td>
			<td width=600 style="background: #F5BEb3"><center><b>Control Flow Diagram</b></center></td>
		</tr>
		<tr>
			<!--
			<td> 
			1st col. data
			</td>-->
			<td> 
				<!-- Content for adding or editing stages -->
				<?  $style_config = "style=\"height: 360px; width: 340px; overflow: hidden\""; ?>
				<div class="global_info" <? print $style_config ?> >
				<center><h3>Global Info.</h3></center>
				<form name="global_info"> 
					<table>
					<?
						put_input_text("App Name", "in_g_appname", "[app name]");
						put_input_select("Push Method", "in_g_cmdpush", array("c2dm", "sms"));
						put_input_text("AMT Requestor ID", "in_g_rrid", "default");
						put_input_text("AMT Requestor Key", "in_g_rrkey", "default");
						put_input_text("Worker ID", "in_g_wwid", "default");
						put_input_text("Timeout", "in_g_timeout", "24 hours");
					?>
					</table>
				</form>
				</div>

				<div class="stage_info" <? print $style_config ?> >
				<center><h3>Stage Info.</h3></center>
				<form name="stage_info">
					<table>
						<? 
						put_input_text("Stage Name", "in_s_name", "[stage name]");
						put_input_select("Stage Type", "in_s_type", array("SPC", "HIT"));
						put_input_text("Binary(Medusalet)", "in_s_binary", "[medusalet name]");
						put_input_text("Voice Instruction", "in_s_inst", "");
						put_input_select("Trigger", "in_s_trigger"
										, array("user-initiated", "immediate"));
						put_input_select("Review", "in_s_review"
										, array("none", "yesno", "labeling", "textdesc"));
						put_input_select("Preview", "in_s_preview"
										, array("user-initiated", "none")); 
						put_input_text("INPUT (Var)", "in_s_input", "");
						?>
					</table>
	
					<!-- SPC specific -->
					<div class="stage_info_spc_config">
						<table>
						<?
							put_input_text("OUTPUT (Var)", "in_s_output", "");
							put_input_text("Parameters", "in_s_params", "");
						?>
						</table>
					</div>
		
					<!-- HIT specific -->
					<div class="stage_info_hit_config">
						<table>
						<?
							put_input_text("OUTPUT (Var)", "in_s_output", "W_WID");
							put_input_text("Statement", "in_s_stmt", "[Instruction for AMT Post]");
							put_input_text("Reward(U.S. $)", "in_s_reward", "0.01");
							put_input_text("Expiration Date", "in_s_expire", date("23:59:59 m/d/Y", strtotime("+7 day")));
						?>
						</table>
					</div>
				</form>
				</div> <!-- stage_info layer -->

				<div class="conn_info" <? print $style_config ?> >
				<center><h3>Connector Info.</h3></center>
				<form name="conn_info">
					<table>
					<?
						put_input_text("Src. Node", "in_c_src", "");
						put_input_text("Primary Dst.(Success)", "in_c_dst_success1", "");
						put_input_text("Primary Dst.(Failure)", "in_c_dst_failure1", "");
						put_input_text("Secondary Dst.(Success)", "in_c_dst_success2", "");
						put_input_text("Secondary Dst.(Failure)", "in_c_dst_failure2", "");
					?>
					</table>
				</form>
				</div>

				<br>

				<div class="next_button">
					<center>
					<input id="prev" type="button" value="Prev"> 
					<input id="save" type="button" value="Save"> 
					<input id="next" type="button" value="Next"> 
					</center>
				</div>
			</td>
			<td> <!-- Content on the control-flow graph -->
				<div class="control_flow_diagram" 
					 style="vertical-align: middle; left: 10px; right: 10px; height: 420px; width: 545px; overflow-x: auto; overflow-y: hidden">
				</div>
			</td>
		</tr>
	<table>

	<!-- place holder -->
	<table border=1>
		<tr><td>
			<div class="log" style="margin-left:10px; height: 100px; width: 880px; overflow-x: hidden; overflow-y: auto; background: white">
			</div>
		</td></tr>
	</table>

	<form>
		<center>
			<input id="save_program" type="button" value="Save the program">
			<input id="exec_program" type="button" value="Execute the program">
		</center>
	</form>

	<!-- finalize the page -->
	<div class="clearer"></div>
	<div id="breadcrumbcontainer">
		<ul>
			<li><a href="http://enl.usc.edu">ENL Home</a></li>
		</ul>
	</div>
	<div class="clearer"></div>
	
	</div> <!-- end container -->

	<div id="footer">
		<p>&copy; 2012 Embedded Networks Laboratory</p>
	</div>

<br>

</body>



