package org.iupac.fairdata.spec.raman;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.spec.IFDSpecData;
import org.iupac.fairdata.spec.IFDSpecDataRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class IFDRamanSpecData extends IFDSpecData {

	public static final String IFD_TYPE_SPEC_RAMAN = "spec.raman";
	
	public static final String IFD_PROP_SPEC_RAMAN_EXPT_LABEL = "IFD.property.spec.raman.expt.label";

	{
		super.setProperties(new IFDProperty[] {
				new IFDProperty(IFDRamanSpecData.IFD_PROP_SPEC_RAMAN_EXPT_LABEL),
		});
	}
	
	public IFDRamanSpecData() throws IFDException {
		super(null, IFD_TYPE_SPEC_RAMAN);
	}
	
	@Override
	protected IFDSpecDataRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDRamanSpecDataRepresentation(ref, obj, len, type, subtype);

	}	

	@Override
	protected IFDSpecData newInstance() throws IFDException {
		return new IFDRamanSpecData();
	}



}
