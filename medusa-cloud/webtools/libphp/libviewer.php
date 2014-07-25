<?php
// library for web viewing interfaces.
//

function put_input_select($label, $name, $option_array, $with_table = true)
{
	if ($with_table == true) {
    	print "<tr>\n";
    	print "<td width=150>$label:</td>\n";
    	print "<td>\n";
	}

    print "<select name=\"$name\">\n";
    for ($i = 0; $i < count($option_array); $i++) {
        if ($i == 0) {
            print "<option value=\"$option_array[$i]\" selected>$option_array[$i]</option>\n";
        }
        else {
            print "<option value=\"$option_array[$i]\">$option_array[$i]</option>\n";
        }
    }

	if ($with_table == true) {
    	print "</td>\n";
    	print "</tr>\n";
	}
}

function put_input_text($label, $name, $default_val, $with_table = true)
{
	if ($with_table == true) {
   		print "<tr>\n";
   		print "<td width=150>$label:</td>\n";
    	print "<td>\n";
	}

	print "<input type=\"text\" value=\"$default_val\" name=\"$name\" style=\"width:200px\" onclick=\"clear_field(this)\">\n";

	if ($with_table == true) {
		print "</td>\n";
    	print "</tr>\n";
	}
}

?>


