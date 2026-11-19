package ch.unil.softarch.luxurycarrental.ui;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ResourceHandlerWrapper;
import jakarta.faces.application.ResourceWrapper;

/**
 * Adds a version parameter to the URLs of the CSS and JavaScript files
 * ({@code site.css?ln=...&v=1700000000000}). In production mode browsers cache these files for
 * a week; with the version, which changes at every deployment, a new release is loaded at once.
 */
public class VersionedResourceHandler extends ResourceHandlerWrapper {

    /** Changes each time the application is deployed. */
    private static final String VERSION = Long.toString(System.currentTimeMillis(), 36);

    public VersionedResourceHandler(ResourceHandler wrapped) {
        super(wrapped);
    }

    @Override
    public Resource createResource(String resourceName) {
        return versioned(super.createResource(resourceName));
    }

    @Override
    public Resource createResource(String resourceName, String libraryName) {
        return versioned(super.createResource(resourceName, libraryName));
    }

    @Override
    public Resource createResource(String resourceName, String libraryName, String contentType) {
        return versioned(super.createResource(resourceName, libraryName, contentType));
    }

    private static Resource versioned(Resource resource) {
        if (resource == null || !isOwnFile(resource.getResourceName())) {
            return resource;
        }
        return new ResourceWrapper(resource) {
            @Override
            public String getRequestPath() {
                String path = super.getRequestPath();
                return path + (path.contains("?") ? "&" : "?") + "v=" + VERSION;
            }
        };
    }

    private static boolean isOwnFile(String name) {
        return name != null && (name.endsWith(".css") || name.endsWith(".js"))
                && (name.startsWith("css/") || name.startsWith("js/"));
    }
}
