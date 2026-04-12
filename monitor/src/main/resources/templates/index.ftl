<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>FreeACS | Module Monitor</title>
	<link rel="stylesheet" href="/css/style.css">
	<#if async??>
		<meta http-equiv="refresh" content="${async}">
	</#if>
	<style>
		body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; color: #333; margin: 20px; }
		fieldset { border: none; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
		legend { font-size: 1.5em; font-weight: bold; color: #2c3e50; margin-bottom: 10px; }

		.monitor-table { width: 100%; border-collapse: collapse; margin-top: 15px; background: #fff; }
		.monitor-table th { background-color: #2c3e50; color: #fff; text-align: left; padding: 12px 15px; text-transform: uppercase; font-size: 0.85em; }
		.monitor-table td { padding: 12px 15px; border-bottom: 1px solid #eee; font-size: 0.95em; }
		.monitor-table tr:hover { background-color: #f9f9f9; }

		/* Status Badges */
		.badge { padding: 4px 8px; border-radius: 12px; font-size: 0.8em; font-weight: bold; text-transform: uppercase; }
		.status-ok { color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; }
		.status-error, .status-fatal { color: #721c24; background-color: #f8d7da; border: 1px solid #f5c6cb; }
		.status-warning { color: #856404; background-color: #fff3cd; border: 1px solid #ffeeba; }
		.status-pending { color: #383d41; background-color: #e2e3e5; border: 1px solid #d6d8db; }

		.refresh-container {
			font-size: 0.9em;
			color: #7f8c8d;
			font-weight: bold;
		}

		#countdown {
			display: inline-block;
			min-width: 25px;
			text-align: center;
			font-family: monospace;
			font-size: 1.1em;
		}


		footer { margin-top: 20px; font-size: 0.8em; color: #7f8c8d; }
	</style>

</head>
<body>
<header>
	<h2>FreeACS Monitoring System</h2>
</header>

<main>
	<#include "${main}">
</main>

<#-- Footer with generation timestamp -->
<hr />
<footer style="font-size: 0.8em; color: #666; text-align: right;">
	<p>@</p>
</footer>
</body>
</html>
