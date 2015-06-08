package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;
import org.pegdown.Extensions;
import org.pegdown.LinkRenderer;
import org.pegdown.PegDownProcessor;
import org.pegdown.VerbatimSerializer;
import org.pegdown.plugins.PegDownPlugins;
import org.pegdown.plugins.ToHtmlSerializerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by ndw on 4/14/15.
 */

@XMLCalabash(
        name = "cx:pegdown",
        type = "{http://xmlcalabash.com/ns/extensions}pegdown")

public class PegDown extends DefaultStep {
    /* Attributes */
    private static final QName _max_parsing_time = new QName("", "max-parsing-time");
    private static final QName _smarts = new QName("", "smarts");
    private static final QName _quotes = new QName("", "quotes");
    private static final QName _abbreviations = new QName("", "abbreviations");
    private static final QName _hardwraps = new QName("", "hardwraps");
    private static final QName _autolinks = new QName("", "autolinks");
    private static final QName _tables = new QName("", "tables");
    private static final QName _definitions = new QName("", "definitions");
    private static final QName _fenced_code_blocks = new QName("", "fenced-code-blocks");
    private static final QName _wikilinks = new QName("", "wikilinks");
    private static final QName _strikethrough = new QName("", "strikethrough");
    private static final QName _anchorlinks = new QName("", "anchorlinks");
    private static final QName _suppress_html_blocks = new QName("", "suppress-html-blocks");
    private static final QName _suppress_inline_html = new QName("", "suppress-inline-html");
    private static final QName _suppress_all_html = new QName("", "suppress-all-html");
    private static final QName _plugins = new QName("", "plugins");
    private static final QName _link_renderer = new QName("", "link-renderer");
    private static final QName _verbatim_serializer = new QName("", "verbatim-serializer");
    private static final QName _to_html_serializer_plugins = new QName("", "to-html-serializer-plugins");

    private static final String library_xpl = "http://xmlcalabash.com/extension/steps/pegdown.xpl";
    private static final String library_url = "/com/xmlcalabash/extensions/pegdown/library.xpl";
    private static final QName _content_type = new QName("content-type");
    private ReadablePipe source = null;
    private WritablePipe result = null;

    public PegDown(XProcRuntime runtime, XAtomicStep step) {
        super(runtime, step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader();
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        XdmNode doc = source.read();
        XdmNode root = S9apiUtils.getDocumentElement(doc);
        String markdown = null;

        if ((XProcConstants.c_data.equals(root.getNodeName())
                && "application/octet-stream".equals(root.getAttributeValue(_content_type)))
                || "base64".equals(root.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(root.getStringValue());
            markdown = new String(decoded);
        } else {
            markdown = root.getStringValue();
        }

        long maxParseTime = Long.MAX_VALUE;
        RuntimeValue option = getOption(_max_parsing_time);
        if (option != null) {
            maxParseTime = Long.parseLong(option.getString());
        }

        PegDownPlugins plugins = null;
        option = getOption(_plugins);
        if (option != null) {
            try {
                plugins = (PegDownPlugins) Class.forName(option.getString()).newInstance();
            } catch (Exception e) {
                throw new XProcException(e);
            }
        }

        LinkRenderer linkRenderer = null;
        option = getOption(_link_renderer);
        if (option != null) {
            try {
                linkRenderer = (LinkRenderer) Class.forName(option.getString()).newInstance();
            } catch (Exception e) {
                throw new XProcException(e);
            }
        }

        Map<String, VerbatimSerializer> verbatimSerializers = null;
        option = getOption(_verbatim_serializer);
        if (option != null) {
            verbatimSerializers = new HashMap<String, VerbatimSerializer>();
            String[] tokens = option.getString().split("\\s+");
            int pos = 0;
            while (pos < tokens.length) {
                String name = tokens[pos];
                String className = tokens[pos + 1];
                pos = pos + 2;
                try {
                    VerbatimSerializer serializer = (VerbatimSerializer) Class.forName(className).newInstance();
                    verbatimSerializers.put(name, serializer);
                } catch (Exception e) {
                    throw new XProcException(e);
                }
            }
        }

        List<ToHtmlSerializerPlugin> toHtml = null;
        option = getOption(_to_html_serializer_plugins);
        if (option != null) {
            toHtml = new Vector<ToHtmlSerializerPlugin>();
            String[] tokens = option.getString().split("\\s+");
            for (String className : tokens) {
                try {
                    ToHtmlSerializerPlugin plugin = (ToHtmlSerializerPlugin) Class.forName(className).newInstance();
                    toHtml.add(plugin);
                } catch (Exception e) {
                    throw new XProcException(e);
                }
            }
        }

        int options = Extensions.NONE;

        if (getOption(_abbreviations, false)) {
            options |= Extensions.ABBREVIATIONS;
        }

        if (getOption(_smarts, false)) {
            options |= Extensions.SMARTS;
        }

        if (getOption(_quotes, false)) {
            options |= Extensions.QUOTES;
        }

        if (getOption(_abbreviations, false)) {
            options |= Extensions.ABBREVIATIONS;
        }

        if (getOption(_hardwraps, false)) {
            options |= Extensions.HARDWRAPS;
        }

        if (getOption(_autolinks, false)) {
            options |= Extensions.AUTOLINKS;
        }

        if (getOption(_tables, false)) {
            options |= Extensions.TABLES;
        }

        if (getOption(_definitions, false)) {
            options |= Extensions.DEFINITIONS;
        }

        if (getOption(_fenced_code_blocks, false)) {
            options |= Extensions.FENCED_CODE_BLOCKS;
        }

        if (getOption(_wikilinks, false)) {
            options |= Extensions.WIKILINKS;
        }

        if (getOption(_strikethrough, false)) {
            options |= Extensions.STRIKETHROUGH;
        }

        if (getOption(_anchorlinks, false)) {
            options |= Extensions.ANCHORLINKS;
        }

        if (getOption(_suppress_html_blocks, false)) {
            options |= Extensions.SUPPRESS_HTML_BLOCKS;
        }

        if (getOption(_suppress_inline_html, false)) {
            options |= Extensions.SUPPRESS_INLINE_HTML;
        }

        if (getOption(_suppress_all_html, false)) {
            options |= Extensions.SUPPRESS_ALL_HTML;
        }

        PegDownProcessor pegDown = null;
        if (plugins == null) {
            pegDown = new PegDownProcessor(options, maxParseTime);
        } else {
            pegDown = new PegDownProcessor(options, maxParseTime, plugins);
        }

        String htmlmarkdown = pegDown.markdownToHtml(markdown);

        HtmlDocumentBuilder htmlBuilder = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET);
        htmlBuilder.setEntityResolver(runtime.getResolver());
        try {
            InputSource src = new InputSource(new StringReader(htmlmarkdown));
            Document html = htmlBuilder.parse(src);
            DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
            XdmNode htmldoc = builder.build(new DOMSource(html));
            result.write(htmldoc);
        } catch (Exception e) {
            throw new XProcException(e);
        }
    }

    public static void configureStep(XProcRuntime runtime) {
        XProcURIResolver resolver = runtime.getResolver();
        URIResolver uriResolver = resolver.getUnderlyingURIResolver();
        URIResolver myResolver = new StepResolver(uriResolver);
        resolver.setUnderlyingURIResolver(myResolver);
    }

    private static class StepResolver implements URIResolver {
        Logger logger = LoggerFactory.getLogger(PegDown.class);
        URIResolver nextResolver = null;

        public StepResolver(URIResolver next) {
            nextResolver = next;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            try {
                URI baseURI = new URI(base);
                URI xpl = baseURI.resolve(href);
                if (library_xpl.equals(xpl.toASCIIString())) {
                    URL url = PegDown.class.getResource(library_url);
                    logger.debug("Reading library.xpl for cx:pegdown from " + url);
                    InputStream s = PegDown.class.getResourceAsStream(library_url);
                    if (s != null) {
                        SAXSource source = new SAXSource(new InputSource(s));
                        return source;
                    } else {
                        logger.info("Failed to read " + library_url + " for cx:pegdown");
                    }
                }
            } catch (URISyntaxException e) {
                // nevermind
            }

            if (nextResolver != null) {
                return nextResolver.resolve(href, base);
            } else {
                return null;
            }
        }
    }
}

