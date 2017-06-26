package foundation.privacybydesign.email.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit filter, that denies the request if the most recent request to the same
 * path was too recent.
 */
@RateLimit
public class RateLimitRequestFilter implements ContainerRequestFilter {
	private static Logger logger = LoggerFactory.getLogger(RateLimitRequestFilter.class);

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>
			requests = new ConcurrentHashMap<>();

	@Context
	private HttpServletRequest servletRequest;

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		int limit = 10;

		String ip = servletRequest.getRemoteAddr();
		String path = servletRequest.getPathInfo();
		Long time = System.currentTimeMillis();

		if (!requests.containsKey(path))
			requests.put(path, new ConcurrentHashMap<String, Long>());

		ConcurrentHashMap<String, Long> accesses = requests.get(path);
		if (!accesses.containsKey(ip)) {
			accesses.put(ip, time);
		} else {
			if (accesses.get(ip) - time < limit * 1000) {
				accesses.put(ip, time);
				logger.warn("Denying request to {} from {}!", path, ip);
				throw new WebApplicationException(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
			}
		}
	}
}
