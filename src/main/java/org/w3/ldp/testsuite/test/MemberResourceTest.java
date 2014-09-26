package org.w3.ldp.testsuite.test;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.jayway.restassured.response.Response;

/**
 * Tests that run on an LDP-RS that is not a container.
 */
public class MemberResourceTest extends RdfSourceTest {
	private static final String SETUP_ERROR = "ERROR: Could not create test resource for MemberResourceTest. Skipping tests.";

	private String container;
	private String memberResource;

	@Parameters("auth")
	public MemberResourceTest(@Optional String auth) throws IOException {
		super(auth);
	}

	@Parameters({"memberResource", "directContainer", "indirectContainer", "basicContainer", "memberTtl"})
	@BeforeSuite(alwaysRun = true)
	public void setup(@Optional String memberResource, @Optional String directContainer,
			@Optional String indirectContainer, @Optional String basicContainer,
			@Optional String memberTtl) {
		// If resource is defined, use that. Otherwise, fall back to creating one from one of the containers.
		if (memberResource != null) {
			this.memberResource = memberResource;
		} else if (directContainer != null) {
			this.container = directContainer;
		} else if (indirectContainer != null) {
			this.container = indirectContainer;
		} else if (basicContainer != null) {
			this.container = basicContainer;
		} else {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"No memberResource or container parameters defined in testng.xml", skipLog);
		}

		if (this.memberResource == null) {
			try {
				Model model = this.readModel(memberTtl);
				if (model == null) {
					model = this.getDefaultModel();
				}

				Response postResponse = buildBaseRequestSpecification()
					.with()
						.contentType(TEXT_TURTLE)
						.body(model, new RdfObjectMapper())
					//.expect()
					//	.header(LOCATION, CoreMatchers.nullValue())
					//	.statusCode(HttpStatus.SC_CREATED)
					.post(this.container);

				if (postResponse.getStatusCode() != HttpStatus.SC_CREATED) {
					throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
							"Could not create test resource for MemberResourceTest, POST failed with status code: " + postResponse.getStatusCode(), skipLog);
				}

				this.memberResource = postResponse.getHeader(LOCATION);
				if (this.memberResource == null) {
					throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
							"Could not create test resource for MemberResourceTest, Location response header missing.", skipLog);
				}
			} catch (Exception e) {
				// Assert.fail(SETUP_ERROR, e);
				throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
						SETUP_ERROR, skipLog);
			}
		}
	}

	@Override
	protected String getResourceUri() {
		if (memberResource == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"Skipping test because test resource is null.", skipLog);
		}
		return memberResource;
	}

	@AfterSuite(alwaysRun = true)
	public void tearDown() {
		// If container isn't null, we created the resource ourselves. To clean up, delete the resource.
		if (container != null) {
			buildBaseRequestSpecification().delete(memberResource);
		}
	}
}
