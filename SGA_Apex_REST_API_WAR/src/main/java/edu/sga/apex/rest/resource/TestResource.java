package edu.sga.apex.rest.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.common.model.multipart.InMultiPart;
import org.apache.wink.common.model.multipart.InPart;

import edu.sga.apex.bean.InputFileBean;
import edu.sga.apex.bean.SubmitJobRequestBean;
import edu.sga.apex.rest.jaxb.InputFile;
import edu.sga.apex.rest.jaxb.ObjectFactory;
import edu.sga.apex.rest.jaxb.SubmitJobRequest;
import edu.sga.apex.rest.jaxb.SubmitJobResponse;
import edu.sga.apex.rest.util.Constants;
import edu.sga.apex.rest.util.ExceptionUtil;
import edu.sga.apex.rest.util.JAXBManager;

@Path("/test")
public class TestResource {
	
	@GET
	@Path("redirect")
	@Produces({ MediaType.TEXT_HTML })
	public Response monitorJob() {
		ResponseBuilder builder = null;
		try {
			URI response = new URI("https://google.com");
			/* Build the response */
			builder = Response.temporaryRedirect(response);
		} catch (Exception ex) {
			/* handle exception and return response */
			return ExceptionUtil.handleException(ex);
		}

		/* Return the response */
		return builder.build();
	}
	
	@GET
	@Path("dummy")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response test() {
		ResponseBuilder builder = null;
		try {
			
			SubmitJobRequest req = new SubmitJobRequest();
			req.setEmailId("email");
			req.setJobName("jobName");
			
			List<InputFile> files = new ArrayList<InputFile>();
			InputFile file = new InputFile();
			file.setFileName("/c/d/e/f");
			file.setFileType("T1");
			files.add(file);
			
			file = new InputFile();
			file.setFileName("/t/y/u/v");
			file.setFileType("T2");
			files.add(file);
			
			req.getInputFiles().addAll(files);
			
			/* Build the response */
			builder = Response.ok(req);
		} catch (Exception ex) {
			/* handle exception and return response */
			return ExceptionUtil.handleException(ex);
		}

		/* Return the response */
		return builder.build();
	}
	
	@Path("upload")
	@POST
	@Consumes( {MediaType.MULTIPART_FORM_DATA})
	public Response fileUpload(InMultiPart inMultiPart) throws Exception {
		String outFileName = null;
		while(inMultiPart.hasNext()) {
		    InPart part = inMultiPart.next();
		    String fileName = null;
		    String[] contentDispositionHeader = part.getHeaders().getFirst("Content-Disposition").split(";");
		      for (String name : contentDispositionHeader) {
		        if ((name.trim().startsWith("filename"))) {
		          String[] tmp = name.split("=");
		          fileName = tmp[1].trim().replaceAll("\"",""); 
		          System.out.println(fileName);
		        }
		      }
		    outFileName = this.createTempFile(part.getInputStream(), fileName);
		}
		
		return Response.ok(outFileName).build();	    
	}
	
	@POST
	@Path("/submit")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response submitRemoteJob(SubmitJobRequest request) {
		ResponseBuilder builder = null;
		ObjectFactory factory = new ObjectFactory();
		try {
			if (request != null) {
				/* Convert request jaxb to middleware req bean */
				SubmitJobRequestBean bean = JAXBManager
						.getSubmitJobRequestBean(request);
				
				
				for(InputFileBean ipFile : bean.getInputFiles()) {
					System.out.println(ipFile);
					File file = new File(Constants.TEMP_DIR, ipFile.getFileName());
					System.out.println("File path: " + file.getAbsolutePath());
					System.out.println("File exists: " + file.exists());
				}
				
				/* Construct response jaxb entity */
				SubmitJobResponse response = factory.createSubmitJobResponse();
				response.setStatus(Constants.STATUS_SUBMITTED);
				
				/* Build the response */
				builder = Response.ok(response);
				builder.status(Status.ACCEPTED);
			} else {
				throw new Exception("Invalid API Request (Empty)");
			}
		} catch (Exception ex) {
			/* handle exception and return response */
			return ExceptionUtil.handleException(ex);
		}

		/* Return the response */
		return builder.build();
	}
	
	private String createTempFile(InputStream inputStream, String fileName) {
		OutputStream outputStream = null;
		String outfile = null;
		
		try {
			File file = File.createTempFile(fileName, String.valueOf(System.currentTimeMillis()));
			outputStream = new FileOutputStream(file);

			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			
			outfile = file.getAbsolutePath();
		} 
		catch (IOException ex) {
			System.err.println("Error creating temp file: " + ex);
			ex.printStackTrace();
		} 
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outputStream != null) {
				try {
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// return path of created temp file
		return outfile;
	}
}