/* ZKDifferTestBase.java

	Purpose:
		
	Description:
		
	History:
		12:33 PM 2023/5/11, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.zkoss.lang.Classes;
import org.zkoss.util.resource.Locator;
import org.zkoss.zk.mock.DummyExec;
import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.impl.VolatilePage;
import org.zkoss.zk.ui.metainfo.LanguageDefinition;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.sys.WebAppsCtrl;

/**
 * A base class for Junit to run with a mock ZK environment.
 * @author jumperchen
 */
public class ZKDifferTestBase {

	public static int INIT_BUFFER_SIZE = 1024 * 8;

	private static final PageDefinition pageDefinition = new PageDefinition(
			LanguageDefinition.getByExtension("zul"), new Locator() {
		public String getDirectory() {
			return null;
		}

		public URL getResource(String s) {
			return null;
		}

		public InputStream getResourceAsStream(String s) {
			return null;
		}
	});

	@BeforeEach
	public void beforeEach() {
		DummyExec dummyExec = new DummyExec();
		WebApp webapp = dummyExec.getDesktop().getWebApp();
		ExecutionsCtrl.setCurrent(dummyExec);
		WebAppsCtrl.setCurrent(webapp);
		VolatilePage volatilePage = new VolatilePage(pageDefinition);
		volatilePage.preInit();
		dummyExec.setCurrentPage(volatilePage);
		// simulated for ZK EE
		try {
			Class<?> runTimeClass = Classes.forNameByThread("org.zkoss.zkex.rt.Runtime");
			runTimeClass.getMethod("init", WebApp.class, Boolean.TYPE).invoke(runTimeClass, webapp, true);
		} catch (Throwable e) {
			// ignore
		}
	}

	@AfterEach
	public void afterEach() {
		// reset
		ExecutionsCtrl.setCurrent(null);
		WebAppsCtrl.setCurrent(null);
	}

	public String redraw(Supplier<? extends AbstractComponent> supplier) {
		StringWriter stringWriter = new StringWriter(INIT_BUFFER_SIZE);
		try {
			AbstractComponent zcmp = supplier.get();

			zcmp.setPage((((ExecutionCtrl) Executions.getCurrent()).getCurrentPage()));
			zcmp.redraw(stringWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return trimUuidPart(stringWriter.toString());
	}

	private static String trimUuidPart(String output) {
		return output.replaceAll("'z__[^\']*'", "")
				.replaceAll(",\\$+[\\w]+:[\\w]+", "")
				.replaceAll("\\$+[\\w]+:[\\w]+,?", "");
	}
}
