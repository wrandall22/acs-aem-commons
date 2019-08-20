package com.adobe.acs.commons.rewriter.impl;

import com.adobe.acs.commons.rewriter.ContentHandlerBasedTransformer;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

/**
 * AuthorDialogNamespaceTransformerFactory
 * <p>
 * Rewrites the name attribute for input fields
 * </p>
 *
 * @author raaijmak@adobe.com
 * @since 2019-08-20
 */
@Component(
        service = TransformerFactory.class,
        property = {
                "pipeline.type=author-dialog-namespace-transformer"
        }
)
public class AuthorDialogNamespaceTransformerFactory implements TransformerFactory {

    static final String ATTR_NAME = "name";
    static final String DELIMITER_NAME = "./";
    static final String REQUEST_ATTR = "namespace";

    @Reference
    private SlingSettingsService slingSettings;
    private boolean isAuthor;


    @Activate
    public void init(){
        isAuthor = slingSettings.getRunModes().contains("author");
    }

    @Override
    public Transformer createTransformer() {
        if(isAuthor){
            return new AuthorDialogNamespaceTransformer();
        }else{
            return new ContentHandlerBasedTransformer();
        }
    }

    class AuthorDialogNamespaceTransformer extends ContentHandlerBasedTransformer {

        private SlingHttpServletRequest request;
        private boolean hasNamespace;
        private String namespace;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            super.init(context, config);
            this.request = context.getRequest();

            this.hasNamespace = this.request.getAttribute(REQUEST_ATTR) != null;

            if(this.hasNamespace){
                this.namespace = this.request.getAttribute(REQUEST_ATTR).toString();
            }
        }

        public void startElement(final String namespaceURI, final String localName, final String qName,
                                 final Attributes attrs)
                throws SAXException {
            final Attributes nextAttributes;
            if (!hasNamespace) {
                nextAttributes = attrs;
            } else {
                nextAttributes = namespaceAttributes(namespaceURI, localName, qName, attrs);
            }
            getContentHandler().startElement(namespaceURI, localName, qName, nextAttributes);
        }

        private Attributes namespaceAttributes(final String namespaceURI, final String localName, final String qName, Attributes attrs) {

            AttributesImpl attributes = new AttributesImpl(attrs);

            int indexOfNameAttribute = attributes.getIndex(namespaceURI, ATTR_NAME);

            if(indexOfNameAttribute > -1){
                String name = attributes.getValue(indexOfNameAttribute);
                if(isNotBlank(name)){
                    String type = attributes.getType(indexOfNameAttribute);
                    String extractedName = substringAfter(name, DELIMITER_NAME);
                    String namespacedName = DELIMITER_NAME + namespace + "/" + extractedName;
                    attributes.setAttribute(indexOfNameAttribute, namespaceURI, ATTR_NAME, ATTR_NAME, type, namespacedName);
                }

            }

            return attributes;
        }

    }
}
