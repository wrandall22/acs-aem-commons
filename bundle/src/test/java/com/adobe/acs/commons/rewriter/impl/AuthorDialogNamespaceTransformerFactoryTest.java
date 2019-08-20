package com.adobe.acs.commons.rewriter.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.util.Collections;

import static com.adobe.acs.commons.rewriter.impl.AuthorDialogNamespaceTransformerFactory.ATTR_NAME;
import static com.adobe.acs.commons.rewriter.impl.AuthorDialogNamespaceTransformerFactory.REQUEST_ATTR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthorDialogNamespaceTransformerFactoryTest {


    @Mock
    private SlingSettingsService slingSettings;

    @Mock
    private ProcessingContext context;
    @Mock
    private ProcessingComponentConfiguration configuration;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private ContentHandler contentHandler;
    @Captor
    private ArgumentCaptor<Attributes> attributesArgumentCaptor;

    @InjectMocks
    private AuthorDialogNamespaceTransformerFactory systemUnderTest;

    private Transformer transformer;

    @Before
    public void setUp() throws Exception {

        when(slingSettings.getRunModes()).thenReturn(Collections.singleton("author"));
        systemUnderTest.init();
        when(context.getRequest()).thenReturn(request);

        transformer = systemUnderTest.createTransformer();
        transformer.setContentHandler(contentHandler);

    }

    @Test
    public void test_publish_no_actions() {

        when(slingSettings.getRunModes()).thenReturn(Collections.singleton("publish"));
        systemUnderTest.init();
        when(context.getRequest()).thenReturn(request);

        transformer = systemUnderTest.createTransformer();
        assertFalse(transformer instanceof AuthorDialogNamespaceTransformerFactory.AuthorDialogNamespaceTransformer);
    }

    @Test
    public void test_no_namespace() throws Exception {

        transformer.init(context, configuration);
        AttributesImpl attributes = new AttributesImpl();

        attributes.addAttribute("", ATTR_NAME, ATTR_NAME, "CDATA", "./someKey");

        transformer.startElement("", "input", "input", attributes);

        verify(contentHandler, times(1)).startElement(anyString(), anyString(), anyString(), attributesArgumentCaptor.capture());

        Attributes newAttributes = attributesArgumentCaptor.getValue();

        int index = newAttributes.getIndex("", ATTR_NAME);

        String newNameValue = newAttributes.getValue(index);

        assertEquals("./someKey", newNameValue);
    }


    @Test
    public void test_namespace() throws Exception {

        when(request.getAttribute(REQUEST_ATTR)).thenReturn("mynamespace");

        transformer.init(context, configuration);

        AttributesImpl attributes = new AttributesImpl();

        attributes.addAttribute("", ATTR_NAME, ATTR_NAME, "CDATA", "./someKey");

        transformer.startElement("", "input", "input", attributes);

        verify(contentHandler, times(1)).startElement(anyString(), anyString(), anyString(), attributesArgumentCaptor.capture());

        Attributes newAttributes = attributesArgumentCaptor.getValue();

        int index = newAttributes.getIndex("", ATTR_NAME);

        String newNameValue = newAttributes.getValue(index);

        assertEquals("./mynamespace/someKey", newNameValue);
    }
}