package dev.aleiis.hintforge.eval;

import com.google.inject.Injector;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import com.intuit.graphql.GraphQLStandaloneSetup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SyntaxChecker {

    private Injector injector;
    private XtextResourceSet resourceSet;
    private IResourceValidator validator;

    public SyntaxChecker() {
        this.injector = new GraphQLStandaloneSetup().createInjectorAndDoEMFRegistration();
        this.resourceSet = injector.getInstance(XtextResourceSet.class);
        this.validator = injector.getInstance(IResourceValidator.class);
    }

    public List<String> validate(String code, boolean excludeEtypeErrors) {
        Resource resource = resourceSet.createResource(URI.createURI("dummy:/dummy_" + System.currentTimeMillis() + ".graphql"));
        List<String> issues = new ArrayList<>();
        try {
            resource.load(new ByteArrayInputStream(code.getBytes()), null);
            List<Issue> allIssues = validator.validate(resource, CheckMode.ALL, null);
            if (!allIssues.isEmpty()) {
                Pattern etypeErrorPattern   = Pattern.compile("^Couldn't resolve reference to");  //  E(Class|StructuralFeature|Object)
                Pattern linkingErrorPattern = Pattern.compile("Linking$");
                for (Issue issue : allIssues) {
                    if (excludeEtypeErrors &&
                            issue.getCode() != null && linkingErrorPattern.matcher(issue.getCode()).find() &&
                            issue.getMessage() != null && etypeErrorPattern.matcher(issue.getMessage()).find())
                        continue;
                    issues.add(issue.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return issues;
    }
}