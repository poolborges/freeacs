<#-- Simplified Monitor Table -->
<div class="monitor-container">
	<header class="table-header" style="display: flex; justify-content: space-between; align-items: center;">
		<h2>System Modules Health Check</h2>
		<#if async??>
			<div class="refresh-container">
				Refreshing in <span id="countdown" class="badge status-pending">${async}</span> seconds
			</div>

			<script>
				(function() {
					let seconds = ${async};
					const display = document.getElementById('countdown');

					const timer = setInterval(function() {
						seconds--;
						display.innerText = seconds;

						if (seconds <= 0) {
							clearInterval(timer);
							// Refresh page when hits zero
							window.location.reload();
						}

						// Optional: Change color when getting close (last 5 seconds)
						if (seconds <= 5) {
							display.classList.remove('status-pending');
							display.classList.add('status-error');
						}
					}, 1000);
				})();
			</script>
		</#if>
	</header>

	<table class="monitor-table">
		<thead>
		<tr>
			<th>Module</th>
			<th style="width: 100px; text-align: center;">Status</th>
			<th style="width: 80px;">Version</th>
			<th style="width: 120px;">Last Update</th>
			<th>Details / Error Message</th>
		</tr>
		</thead>
		<tbody>
		<#list events as event>
			<#assign currentStatus = (event.status()!"PENDING")>
			<tr>
				<td><strong>${event.module()}</strong></td>
				<td style="text-align: center;">
                        <span class="badge status-${currentStatus?lower_case}">
                            ${currentStatus}
                        </span>
				</td>
				<td><code>${event.version()!"-"}</code></td>
				<td>${(event.lastUpdate().format(java8TimeFormatter))!"Never"}</td>
				<td style="color: #666; font-style: italic;">
					${event.errorMessage()!"System operational"}
				</td>
			</tr>
		<#else>
			<tr>
				<td colspan="5" style="text-align: center; padding: 30px;">
					No modules are currently being monitored.
				</td>
			</tr>
		</#list>
		</tbody>
	</table>
</div>

<footer>
	Generated on: <strong>${.now?string("yyyy-MM-dd HH:mm:ss")}</strong>
</footer>
