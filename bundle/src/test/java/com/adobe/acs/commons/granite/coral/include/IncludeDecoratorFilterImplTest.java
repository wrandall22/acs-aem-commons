package com.adobe.acs.commons.granite.coral.include;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.granite.coral.include.IncludeDecoratorFilterImpl.NAMESPACE;
import static com.adobe.acs.commons.granite.coral.include.IncludeDecoratorFilterImpl.PARAMETERS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IncludeDecoratorFilterImplTest {


    private HashMap<String,Object> parameters = new HashMap<>();

    @Mock
    private Resource resource;

    @Mock
    private Resource parameterResource;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private IncludeDecoratorFilterImpl systemUnderTest;
    private Map<String, Object> passedAttributes;

    private Map<String,Object> currentRequestAttributes = new HashMap<>();

    @Before
    public void setUp() throws IOException, ServletException {
        when(request.getResource()).thenReturn(resource);
        when(request.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
        when(resource.getChild(PARAMETERS)).thenReturn(parameterResource);
        when(parameterResource.getValueMap()).thenReturn(new ValueMapDecorator(parameters));

        doAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0, String.class);
            Object value = invocationOnMock.getArgument(1, Object.class);
            currentRequestAttributes.put(key, value);
            return null;
        }).when(request).setAttribute(anyString(), any());

        when(request.getAttributeNames()).thenAnswer((Answer<Enumeration<String>>) invocationOnMock ->
                Collections.enumeration(currentRequestAttributes.keySet())
        );

        when(request.getAttribute(anyString())).thenAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0, String.class);
            return currentRequestAttributes.get(key);
        });

        doAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0, String.class);
            currentRequestAttributes.remove(key);
            return null;
        }).when(request).removeAttribute(anyString());

        doAnswer(invocationOnMock -> passedAttributes = ImmutableMap.copyOf(currentRequestAttributes)).when(chain).doFilter(request,response);
    }


    @Test
    public void test() throws IOException, ServletException {

        parameters.put(NAMESPACE, "mynamespace");
        systemUnderTest.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request,response);
        assertFalse(passedAttributes.isEmpty());
        assertTrue(currentRequestAttributes.isEmpty());
        assertEquals("mynamespace", passedAttributes.get(NAMESPACE));

    }

    /**
     * This tests nested include namespace nesting.
     * So if you make a namespaced include in a namespaced include, it should be applied only in it's own context and put back properly afterward.
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void test_with_existing_parameter() throws IOException, ServletException {

        currentRequestAttributes.put("namespace", "currentnamespace");
        parameters.put(NAMESPACE, "mynamespace");
        systemUnderTest.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request,response);
        assertFalse(passedAttributes.isEmpty());
        assertFalse(currentRequestAttributes.isEmpty());
        assertEquals("currentnamespace/mynamespace", passedAttributes.get(NAMESPACE));
        assertEquals("currentnamespace", currentRequestAttributes.get(NAMESPACE));
    }


}