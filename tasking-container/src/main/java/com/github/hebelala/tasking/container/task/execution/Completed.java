/**
 * Copyright © 2019 hebelala (hebelala@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hebelala.tasking.container.task.execution;

import com.github.hebelala.tasking.api.Response;

/**
 * @author hebelala
 */
public class Completed {

	private int round;
	private long bzStartTime;
	private long bzEndTime;
	private Response bzResponse;
	private String bzThrowableString;
	private String throwableString;

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public long getBzStartTime() {
		return bzStartTime;
	}

	public void setBzStartTime(long bzStartTime) {
		this.bzStartTime = bzStartTime;
	}

	public long getBzEndTime() {
		return bzEndTime;
	}

	public void setBzEndTime(long bzEndTime) {
		this.bzEndTime = bzEndTime;
	}

	public Response getBzResponse() {
		return bzResponse;
	}

	public void setBzResponse(Response bzResponse) {
		this.bzResponse = bzResponse;
	}

	public String getBzThrowableString() {
		return bzThrowableString;
	}

	public void setBzThrowableString(String bzThrowableString) {
		this.bzThrowableString = bzThrowableString;
	}

	public String getThrowableString() {
		return throwableString;
	}

	public void setThrowableString(String throwableString) {
		this.throwableString = throwableString;
	}
}
