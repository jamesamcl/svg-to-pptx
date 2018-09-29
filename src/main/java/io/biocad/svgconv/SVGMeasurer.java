
package io.biocad.svgconv;

import java.awt.geom.Rectangle2D;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.anim.dom.SVGOMElement;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.SVG12Constants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLocatable;
import org.w3c.dom.svg.SVGRect;

public class SVGMeasurer
{
    DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
    SVGDocument doc = (SVGDocument) impl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);

    
	UserAgent userAgent;
	DocumentLoader loader;
	BridgeContext ctx;
	GVTBuilder builder;
	GraphicsNode rootGN;
	
	SVGMeasurer()
	{
		userAgent = new UserAgentAdapter();
		loader = new DocumentLoader(userAgent);
		ctx = new BridgeContext(userAgent, loader);
		ctx.setDynamicState(BridgeContext.DYNAMIC);
		builder = new GVTBuilder();
		rootGN = builder.build(ctx, doc);
	}

    public Rectangle2D measureNode(SVGOMElement node)
    {
    	Node tempNode = doc.importNode(node,  true);
    	
    	doc.getDocumentElement().appendChild(tempNode);
    	
    	SVGRect bbox = ((SVGLocatable) tempNode).getBBox();
    	
    	Rectangle2D rect = new Rectangle2D.Double(
    		bbox.getX(), bbox.getY(),
    		bbox.getWidth(), bbox.getHeight()
    	);
    	
    	doc.getDocumentElement().removeChild(tempNode);
    	
    	System.out.println("node is " + rect);
    	
    	return rect;
    }
}
