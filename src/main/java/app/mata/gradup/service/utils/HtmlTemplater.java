package app.mata.gradup.service.utils;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
public class HtmlTemplater {

  private final TemplateEngine templateEngine;

  public HtmlTemplater() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    this.templateEngine = new TemplateEngine();
    this.templateEngine.setTemplateResolver(resolver);
  }

  public String render(String templateName, Context context) {
    return templateEngine.process(templateName, context);
  }
}
