package org.iupac.fairdata.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.iupac.fairdata.api.IFDObjectI;
import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;

/**
 * IFDObject is the base abstract superclass for all IFD Data Model metadata objects.
 * 
 * IFDObject extends ArrayList so as to allow for storing and retrieving
 * multiple objects or representations with standard ArrayList methods.
 * 
 * An IFDObject can be initialized with just an arbitrary name or with a name
 * and a maximum count, or with a name, a maximum count, and an "immutable"
 * starting set that are not allowed to be set or removed or changed.
 * 
 * IFDObject and its subclasses implement the IFDObjectI interface and come in
 * two flavors: IFDRepresentableObject and IFDCollection.
 * 
 * 
 * *** IFDRepresentableObject ***
 * 
 * IFDStructure, IFDDataObject, IFDSample, IFDStructureAnalysis,
 * IFDSampleAnalysis
 * 
 * A class extending IFDRepresentableObject is expected to have one or more
 * distinctly different digital representations (byte sequences) that amount to
 * more than just metadata. These are the 2D or 3D MOL or SDF models, InChI or
 * SMILES strings representing chemical structure, and the experimental,
 * predicted, or simulated NMR or IR data associated with a chemical compound or
 * mixture of compounds.
 * 
 * The representations themselves, in the form of subclasses of
 * IFDRepresentation, do not implement IFDObjectI. (They are not themselves
 * inherently lists of anything.) They could be any Java Object, but most likely
 * will be of the type String or byte[] (byte array). They represent the actual
 * "Digital Items" that might be found in a ZIP file or within a cloud-based or
 * local file system directory.
 * 
 * Note that there is no mechanism for IFDStructure, IFDSample, or IFDDataObject
 * objects to refer to each other. This is a key aspect of the IFD Data Model.
 * The model is based on the idea of a collection, and it is the IFDCollection
 * objects that do this referencing. This model allows for a relatively simple
 * and flexible packaging of objects, with their relationships identified only
 * in key metadata resources.
 * 
 * *** IFDCollection ***
 * 
 * IFDCollection objects are "pure metadata" and thus have no digital
 * representation outside of that role. They point to and associate IFDObject
 * instances. IFDCollection objects may be collections of IFDCollection objects.
 * This allows for a nesting of collections in meaningful ways. For example,
 * IFDStructureDataAssociation objects maintain two specific collections: one of
 * structures, and one of data objects.
 * 
 * To be sure, an IFDCollection could represent a Digital Object in the form of
 * a zip file (and usually does) or perhaps a finding aid. Nonetheless, this
 * does not make it "representable" in the sense that it is likely to have
 * multiple distinctly different representations that characterize
 * IFDRepresentableObject classes.
 * 
 * 
 * *** core IFDObjectI classes ***
 * 
 * -- IFDStructure --
 * 
 * A chemically-related structural object, which may have several
 * representations, such as a 2D or 3D MOL file, an InChI or InChIKey, one or
 * more chemical names, one or more SMILES strings, or even just a PNG image of
 * a drawn structure. Each of these representations serves a purpose. Some are
 * more "interoperable" than others, but each in its own way may be more useful
 * in a given context.
 * 
 * -- IFDDataObject --
 * 
 * A data object references one or more Digital Objects that are what a
 * scientist would call "their data", such as a full vendor experiment dataset
 * (a Bruker NMR experiment), a PNG image of a spectrum, or a peaklist.
 * 
 * For the IUPAC FAIRSpec Project, we extend this class to IFDSpecData and its
 * subclasses (IFDNMRSpecData, IFDIRSpecData, etc.). We recognize, however, that
 * the system we are developing is not limited to spectroscopic data only, and
 * future implementations of this data model may subclass IFDDataObject in
 * completely different ways for completely different purposes.
 * 
 * -- IFDSample --
 * 
 * IFDSample corresponds to a specific physical sample that may or may not (yet)
 * have a chemical structure, spectroscopic data, or representations associated
 * with it.
 * 
 * -- IFDStructureCollection and IFDDataObjectCollection --
 * 
 * These two classes each collect distinctly different structures and data,
 * respectively, that are related in some way. For instance, all the compounds
 * referred to in a publication, all the spectra for a publication, or all the
 * spectra relating to a specific compound. (Or, perhaps, all the compounds in a
 * mixture that are identified in an NMR spectrum.)
 * 
 * 
 * *** associative IFDObjectI classes ***
 *
 * The org/iupac/fairdata/assoc package includes four higher-level abstract
 * IFDObjectI classes: IFDStructureDataAssociation, IFDSampleDataAssociation,
 * and their collections. These classes associate specific structures, samples,
 * and data to each other. In addition, the IFDAnalysis-related classes and the
 * IFDFindingAid class fall into this category.
 * 
 * 
 * -- IFDStructureDataAssociation --
 * 
 * This class correlates one or more IFDStructure instances with one or more
 * IFDDataObject instances. It provides the "connecting links" between spectra
 * and structure.
 * 
 * -- IFDSampleAssociation --
 * 
 * This class correlates one IFDSample instance with one or more IFDDataObject
 * and IFDStructure instances. It provides a way of linking samples with their
 * associated molecular structure (if known) and spectra (if taken).
 * 
 * -- IFDStructureAnalysis --
 * 
 * The IFDAnalysis class is intended to represent a detailed correlation between
 * chemical structure for a compound and its related experimental or theoretical
 * spectroscopic data. For instance, it might correlate specific atoms or groups
 * of atoms of a chemical structure with specific signals in a spectrum or other
 * sort of data object.
 *
 * -- IFDSampleAnalysis --
 * 
 * The IFDSampleAnalysis class is intended to represent a detailed correlation
 * between a specific chemical sample and its structure and spectroscopic data.
 * The details of this analysis would be designed into the subclass being
 * implemented.
 *
 * -- IFDFindingAid --
 *
 * In general, an overall collection will contain a "master" collection metadata
 * object, the IFDFindingAid. The IFDFindingAid is a special "collection of
 * collections," providing:
 * 
 * 1) metadata relating to the entire collection. For example, a publication or
 * thesis.
 * 
 * 2) high-level access to lower-level metadata. For example, a list of compound
 * names or key spectroscopy data characteristics such as NMR nucleus (1H, 13C,
 * 31P) and spectrometer nominal frequency (300 MHz, 800 MHz) or type of IR
 * analysis (such as ATR).
 * 
 * 3) pointers to finding aids for subcollections, each of which which may be a
 * pointer to one or more additional finding aids.
 * 
 * It is the IFDFindingAid that ultimately distinguishes the IUPAC FAIRSpec data
 * model from other models. It should contain all the information that forms the
 * basis of what the user sees. It should reveal information about the
 * collection that allows users to quickly determine whether data in this
 * collection are relevant to their interests or not. The IFDFindingAid could be
 * static -- a Digital Item within a repository collection -- or dynamically
 * created in response to a query.
 * 
 * 
 * @author hansonr
 *
 * @param <T> the class for items of the list - IFDRepresentations for
 *            DataObjects and Structures; relevant IFDObject types for
 *            IFDCollections
 */
@SuppressWarnings("serial")
public abstract class IFDObject<T> extends ArrayList<T> implements IFDObjectI<T>, IFDSerializableI {

	public final static String REP_TYPE_UNKNOWN = "unknown";

	protected static int indexCount;

	/**
	 * a unique identifier for debugging
	 */
	protected int index;

	/**
	 * index of source URL in the IFDFindingAid URLs list; must be set nonnegative
	 * to register
	 */
	private int sourceIndex = -1;

	/**
	 * an arbitrary name given to provide some sort of context
	 */
	protected String name;

	/**
	 * an arbitrary identifier to provide some sort of context
	 */
	protected String id;

	/**
	 * an arbitrary path to provide some sort of context
	 */
	protected String path;

	/**
	 * known properties of this class, fully identified in terms of data type and
	 * units
	 * 
	 */
	protected final Map<String, IFDProperty> htProps = new TreeMap<>();

	/**
	 * generic properties that could be anything but are not in the list of known
	 * properties
	 */
	protected Map<String, Object> params = new TreeMap<>();

	/**
	 * the maximum number of items allowed in this list; may be 0
	 */
	private final int maxCount;
	/**
	 * the minimum number of items allowed in this list, set by initializing it with
	 * a set of "fixed" items
	 */
	private int minCount;

	protected final String classType;

	protected String subtype = ObjectType.Unknown;

	@SuppressWarnings("unchecked")
	public IFDObject(String name, String type) throws IFDException {
		this(name, type, Integer.MAX_VALUE);
	}

	@SuppressWarnings("unchecked")
	public IFDObject(String name, String type, int maxCount, T... initialSet) throws IFDException {
		this.name = name;
		if (type == null)
			throw new IFDException("IFDObject must have a type");
		this.classType = type;
		this.maxCount = maxCount;
		this.index = indexCount++;
		if (initialSet == null) {
			minCount = 0;
		} else {
			minCount = initialSet.length;
			for (int i = 0; i < minCount; i++)
				super.add(initialSet[i]);
		}
	}

	/**
	 * Set the minimum count of list objects such that an attempt to remove one of
	 * these objects will result in an IFDException.
	 * 
	 * @param n
	 */
	protected void setMinCount(int n) {
		minCount = n;
	}

	/**
	 * Generally set only by subclasses during construction, these IFDProperty
	 * definitions are added to the htProps list.
	 * 
	 * @param properties
	 */
	protected void setProperties(IFDProperty[] properties) {
		for (int i = properties.length; --i >= 0;)
			htProps.put(properties[i].getName(), properties[i]);
	}

	public Map<String, IFDProperty> getProperties() {
		return htProps;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setPropertyValue(String name, Object value) {
		// check for .representation., which is not stored in the object.
		if (IFDConst.isRepresentation(name))
			return;
		if (IFDConst.isLabel(name))
			this.name = value.toString();
		IFDProperty p = htProps.get(name);
		if (p == null) {
			if (value == null)
				params.remove(name);
			else
				params.put(name, value);
			return;
		}
		htProps.put(name, p.getClone(value));
	}

	public Object getPropertyValue(String name) {
		IFDProperty p = htProps.get(name);
		return (p == null ? params.get(name) : p.getValue());
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUrlIndex(int urlIndex) {
		this.sourceIndex = urlIndex;
	}

	public int getUrlIndex() {
		return sourceIndex;
	}

	public String getID() {
		return (id == null ? "" + index : id);
	}

	public void setID(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public T getObject(int index) {
		return get(index);
	}

	@Override
	public String getObjectType() {
		return classType;
	}

	@Override
	public int getObjectCount() {
		return size();
	}

	@Override
	public T remove(int index) {
		checkRange(index);
		return super.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		int i = super.indexOf(o);
		if (i < 0)
			return false;
		checkRange(i);
		return super.remove(o);
	}

	@Override
	public T set(int index, T c) {
		checkRange(index);
		return super.set(index, c);
	}

	/**
	 * Subclasses can bypass the minimum/maximum restrictions.
	 * 
	 * @param index
	 * @param c
	 */
	protected void setSafely(int index, T c) {
		super.set(index, c);
	}

	private void checkRange(int index) {
		if (index < minCount)
			throw new IndexOutOfBoundsException("operation not allowed for index < " + minCount);
		if (index > maxCount)
			throw new IndexOutOfBoundsException("operation not allowed for index > " + maxCount);
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + " " + index + " " + " size=" + size() + "]";
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 
	 * @return true if any property value is not null
	 */
	public boolean haveProperties() {
		for (IFDProperty p : htProps.values()) {
			if (p.getValue() != null)
				return true;
		}
		return false;
	}

	@Override
	public String getSerializedType() {
		return classType;
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		serializeTop(serializer);
		serializeProps(serializer);
		serializeList(serializer);
	}

	protected void serializeTop(IFDSerializerI serializer) {
		serializer.addAttr("type", getSerializedType());
		if (subtype != null && subtype != ObjectType.Unknown)
			serializer.addAttr("subtype", subtype);
		serializer.addAttr("name", getName());
		serializer.addAttr("id", getID());
	}

	protected void serializeProps(IFDSerializerI serializer) {
		if (sourceIndex >= 0)
			serializer.addAttrInt("sourceIndex", sourceIndex);
		serializer.addAttr("path", getPath());
		if (haveProperties()) {
			// general serialization does not write out units
			Map<String, Object> map = new TreeMap<>();
			for (Entry<String, IFDProperty> e : htProps.entrySet()) {
				Object val = e.getValue().getValue();
				if (val != null)
					map.put(e.getKey(), val);
			}
			serializer.addObject("properties", map);
		}
		if (getParams().size() > 0)
			serializer.addObject("params", getParams());
	}

	protected void serializeList(IFDSerializerI serializer) {
		if (size() > 0) {
			serializer.addAttrInt("listCount", size());
			List<T> list = new ArrayList<T>();
			for (int i = 0, n = size(); i < n; i++)
				list.add(get(i));
			serializer.addObject("list", list);
		}
	}

	@Override
	public boolean equals(Object o) {
		return (o == this);
//		if (!super.equals(o))
//			return false;
//		IFDObject<?> c = (IFDObject<?>) o;
//		return name.equals(c.getName());
	}
}