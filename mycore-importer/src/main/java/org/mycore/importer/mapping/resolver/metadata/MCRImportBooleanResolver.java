package org.mycore.importer.mapping.resolver.metadata;


public class MCRImportBooleanResolver extends MCRImportAbstractMetadataResolver {
    
    @Override
    protected boolean isValid() {
        String text = saveToElement.getText();
        if(Boolean.valueOf(text))
            return true;
        return false;
    }

}