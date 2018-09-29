
package io.biocad.svgconv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.*;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGOMElement;
import org.apache.batik.anim.dom.SVGOMTextElement;
import org.apache.batik.dom.GenericText;
import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.AWTTransformProducer;
import org.apache.batik.parser.PathParser;
import org.apache.batik.parser.TransformListParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.sl.usermodel.TextShape.TextAutofit;
import org.apache.poi.xslf.usermodel.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.awt.Font;
import java.awt.font.FontRenderContext;

public class SVGConverter
{
	final double TEXTBOX_PADDING = 1.2;
	
	XMLSlideShow ppt;
    XSLFSlide slide;
    
    SVGMeasurer measurer;
    
    public SVGConverter()
    {
    	measurer = new SVGMeasurer();
    	
		ppt = new XMLSlideShow();
		slide = ppt.createSlide();
    }
    
	public void write(OutputStream outputStream) throws IOException
	{
		ppt.write(outputStream);
	}
	
	public void convertSVG(InputStream is) throws IOException
	{
		  String parser = XMLResourceDescriptor.getXMLParserClassName();
	    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
	    Document doc = f.createSVGDocument("http://converted", is);
	    
	    NodeList children = doc.getChildNodes();
	    
	    for(int i = 0; i < children.getLength(); ++ i)
	    {
	    	Node node = children.item(i);
	    	
	    	String localName = node.getLocalName();
	    	
	    	if(localName == null)
	    		continue;
	    	
	    	if(node.getLocalName().equals("svg"))
	    	{
	    		visitRootNode(node);
	    	}

	    }
	    
	}
	
	public void visitRootNode(Node node)
	{
		Node widthNode = node.getAttributes().getNamedItem("width");
		Node heightNode = node.getAttributes().getNamedItem("height");
		
		if(widthNode != null && heightNode != null)
		{
			ppt.setPageSize(
					new Dimension(
							(int) Math.round(dimToDouble(widthNode.getTextContent())),
							(int) Math.round(dimToDouble(heightNode.getTextContent()))
							)
					);
									
		}
		//node.getAttributes().getNamedItem("width")
		//ppt.setPageSize(new Dimension());
		
		
		
		if(node.getLocalName().equals("title"))
		{
			return;
		}
		
		if(node.getLocalName().equals("desc"))
		{
			return;
		}
		
		VisitorState state = new VisitorState(new AffineTransform(), null);
		
		visitNode(state, node);
	}
	
	public void visitNode(VisitorState state, Node node)
	{
		if(node == null)
			return;
		
		state = state.clone();
		
		NamedNodeMap attr = node.getAttributes();
		
		if(attr != null)
		{
			Node transformString = node.getAttributes().getNamedItem("transform");
			
			if(transformString != null)
			{
				System.out.println(transformString.getNodeValue());

				System.out.println(attrToTransform(transformString.getNodeValue()));
				
				state.transform.concatenate(
						attrToTransform(
								transformString.getNodeValue()));
			}
		}
		
		
		Rectangle2D rect = getNodeRect(node);
		
		String tagName = node.getLocalName();
		
		if(node instanceof GenericText)
		{
			String text = node.getTextContent().trim();
			
			if(text.length() > 0)
			{
				if(state.textShape != null)
				{
					state.textShape.clearText();
					
					XSLFTextParagraph paragraph = state.textShape.addNewTextParagraph();
	
					//paragraph.setTextAlign(TextAlign.CENTER);
					
				    XSLFTextRun textRun = paragraph.addNewTextRun();
	
				    textRun.setText(text);
				    
				    applyNodeStyleToText(node, textRun);
				
				}
			}
		}
		else if(tagName == "text")
		{
			XSLFTextBox textBox = slide.createTextBox();
			
			state.textShape = textBox;
		
			Rectangle2D actualRect = measurer.measureNode((SVGOMElement) node);
			
			textBox.setAnchor(new Rectangle2D.Double(
					state.transform.getTranslateX() + rect.getX() + actualRect.getX(),
					state.transform.getTranslateY() + rect.getY() + actualRect.getY(),
					actualRect.getWidth() * TEXTBOX_PADDING, actualRect.getHeight() * TEXTBOX_PADDING));
			
			SVGOMTextElement textElem = ((SVGOMTextElement) node);
			
			System.out.println(textElem.getBBox());
			
		}
		else if(tagName == "rect")
		{	
			XSLFFreeformShape shape = slide.createFreeform();
			shape.setPath(new Path2D.Double(rect, state.transform));

			state.textShape = shape;
			
			applyNodeStyleToShape(node, shape);	
		}
		else if(tagName == "line")
		{
			Node nodeX1 = node.getAttributes().getNamedItem("x1");
			Node nodeY1 = node.getAttributes().getNamedItem("y1");
			Node nodeX2 = node.getAttributes().getNamedItem("x2");
			Node nodeY2 = node.getAttributes().getNamedItem("y2");
			
			if(nodeX1 == null || nodeY1 == null || nodeX2 == null || nodeY2 == null)
			{
				return;
			}

			double x1 = Double.parseDouble(nodeX1.getNodeValue());
			double y1 = Double.parseDouble(nodeY1.getNodeValue());
			double x2 = Double.parseDouble(nodeX2.getNodeValue());
			double y2 = Double.parseDouble(nodeY2.getNodeValue());
			
			Line2D.Double line = new Line2D.Double(x1, y1, x2, y2);
			
			XSLFFreeformShape shape = slide.createFreeform();
			shape.setPath(new Path2D.Double(line, state.transform));

			state.textShape = shape;
			
			applyNodeStyleToShape(node, shape);
		}
		else if(tagName == "path")
		{  
			PathParser parser = new PathParser();
			
			AWTPathProducer pathProducer = new AWTPathProducer();
			
			parser.setPathHandler(pathProducer);
			
			parser.parse(node.getAttributes().getNamedItem("d").getNodeValue());


			XSLFFreeformShape shape = slide.createFreeform();
			shape.setPath(new Path2D.Double(pathProducer.getShape(), state.transform));

			state.textShape = shape;
			
			applyNodeStyleToShape(node, shape);
			
		}

	    NodeList children = node.getChildNodes();
	    
	    for(int i = 0; i < children.getLength(); ++ i)
	    {
	    	Node childNode = children.item(i);
	    	
	    	visitNode(state, childNode);
	    }
	    

	}
	
	class VisitorState
	{
		public VisitorState(AffineTransform transform, XSLFTextShape textShape)
		{
			this.transform = new AffineTransform(transform);
			this.textShape = textShape;
		}
		
		public AffineTransform transform;
		
		public XSLFTextShape textShape;
	
		public VisitorState clone()
		{
			return new VisitorState(transform, textShape);
		}
	}
	

    AffineTransform attrToTransform(String attr) {
        TransformListParser p = new TransformListParser();
        AWTTransformProducer tp = new AWTTransformProducer();
        p.setTransformListHandler(tp);
        p.parse(attr);
        return tp.getAffineTransform();
    }
    
    Rectangle2D.Double getNodeRect(Node node)
    {
    	NamedNodeMap attr = node.getAttributes();
    	
    	if(attr == null) {
    		return new Rectangle2D.Double();
    	}
    	
    	Node nodeX = attr.getNamedItem("x");
    	Node nodeY = attr.getNamedItem("y");  
    	Node nodeWidth = attr.getNamedItem("width");
    	Node nodeHeight = attr.getNamedItem("height");  
    	
    	double x, y, width, height;
    	
    	x = nodeX != null ? dimToDouble(nodeX.getNodeValue()) : (double) 0;
    	y = nodeY != null ? dimToDouble(nodeY.getNodeValue()) : (double) 0;
    	width = nodeWidth != null ? dimToDouble(nodeWidth.getNodeValue()) : (double) 0;
    	height = nodeHeight != null ? dimToDouble(nodeHeight.getNodeValue()) : (double) 0;
    	
    	Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
    
    	return rect;
    }

    double dimToDouble(String dim)
    {
    	if(dim.endsWith("px"))
    	{
    		return Double.parseDouble(dim.substring(0, dim.length() - 2));
    	}
    	
    	return Double.parseDouble(dim);
    }
    
    void applyNodeStyleToShape(Node node, XSLFFreeformShape shape)
    {
    	NamedNodeMap attr = node.getAttributes();
    	
    	if(attr == null) {
    		return;
    	}
    	
    	/*
			shape.setLineWidth(1);
			shape.setLineColor(Color.BLACK);		
			
			*/
    	
    	Node nodeFill = attr.getNamedItem("fill");
    	
    	if(nodeFill != null)
    	{
    		Color fillColor = convertColor(nodeFill.getNodeValue());
    		
    		if(fillColor != null)
    			shape.setFillColor(fillColor);
    	}
    	

    	Node nodeStroke = attr.getNamedItem("stroke");
    	
    	if(nodeStroke != null)
    	{
    		Color strokeColor = convertColor(nodeStroke.getNodeValue());
    		
    		if(strokeColor != null)
    			shape.setLineColor(strokeColor);
    	}
    	
    	Node nodeStrokeWidth = attr.getNamedItem("stroke-width");
    	
    	if(nodeStrokeWidth != null)
    	{
    		shape.setLineWidth(dimToDouble(nodeStrokeWidth.getNodeValue()));
    	}
    	
    	
    }

    void applyNodeStyleToText(Node node, XSLFTextRun textRun)
    {
		textRun.setFontSize(8.0);
		
    	while(node != null)
    	{
    		NamedNodeMap attr = node.getAttributes();
    		
    		if(attr != null)
    		{
    			Node nodeFontFamily = attr.getNamedItem("font-family");
    			
    			if(nodeFontFamily != null)
    			{
    				String fontFamily = nodeFontFamily.getTextContent();
    				
    				textRun.setFontFamily(fontFamily);
    			}
    			
    			Node nodeFontSize = attr.getNamedItem("font-size");

    			if(nodeFontSize != null)
    			{
    				String fontSize = nodeFontFamily.getTextContent();
    				
    				textRun.setFontSize(8.0);
    			}

    		}
    		
    		node = node.getParentNode();
    	}
    	
    }
    
    Color convertColor(String color)
    {
	    if(color.equals("none")) {
                return null;
	    }
	 
	    // TODO hacky, should have a mapping of SVG color names
	    try {
		return (Color) Color.class.getDeclaredField(color).get(null);
	    } catch(Exception e) {
		return Color.decode(color);
	    }
    }
    
   
    
    

}
