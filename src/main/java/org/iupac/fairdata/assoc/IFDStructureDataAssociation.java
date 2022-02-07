package org.iupac.fairdata.assoc;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.struc.IFDStructure;
import org.iupac.fairdata.struc.IFDStructureCollection;

/**
 * A class to correlation one or more IFDStructure with one or more
 * IFDDataObject. Only two array items are allowed -- one IFDStructureCollection
 * and one IFDDataObjectCollection.
 * 
 * Each of these collections allows for one or more item, resulting in a
 * one-to-one, many-to-one, one-to-many, or many-many associations.
 * 
 * An abstract object that does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDStructureDataAssociation extends IFDCollection<IFDCollection<?>> {
	
	public IFDStructureDataAssociation(String name, String type, IFDStructureCollection structureCollection, IFDDataObjectCollection<?> dataCollection) throws IFDException {
		super(name, type, 2, structureCollection, dataCollection);
		if (dataCollection == null || structureCollection == null)
			throw new IFDException("IFDSample constructure must provide IFDStructureCollection and IFDDataCollection");
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDStructureDataAssociation))
			return false;
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) o;
		return (ss.get(0).equals(get(0)) && ss.get(1).equals(get(1)));
	}

	public IFDStructureCollection getStructureCollection() {
		return (IFDStructureCollection) get(0);
	}

	@SuppressWarnings("unchecked")
	public IFDDataObjectCollection<IFDDataObject<?>> getDataObjectCollection() {
		return (IFDDataObjectCollection<IFDDataObject<?>>) get(1);
	}

	public IFDStructure getStructure(int i) {
		return getStructureCollection().get(i);
	}

	public IFDDataObject<?> getDataObject(int i) {
		return (IFDDataObject<?>) getDataObjectCollection().get(i);
	}

	public IFDStructure getFirstStructure() {
		return getStructureCollection().get(0);
	}

	public IFDDataObject<?> getFirstDataObject() {
		return (IFDDataObject<?>) getDataObjectCollection().get(0);
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		serializer.addObject("struc", getStructureCollection().getIndexList());
		serializer.addObject("data", getDataObjectCollection().getIndexList());
	}
	
	protected boolean addStructure(IFDStructure struc) {
		return getStructureCollection().add(struc);
	}

	protected boolean addDataObject(IFDDataObject<?> data) {
		return getDataObjectCollection().add(data);
	}



}
