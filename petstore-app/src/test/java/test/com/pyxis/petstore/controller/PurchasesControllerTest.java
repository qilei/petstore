package test.com.pyxis.petstore.controller;

import com.pyxis.petstore.controller.PaymentDetails;
import com.pyxis.petstore.controller.PurchasesController;
import com.pyxis.petstore.domain.*;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import static com.pyxis.matchers.spring.SpringMatchers.hasAttribute;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static test.support.com.pyxis.petstore.builders.OrderBuilder.anOrder;

@RunWith(JMock.class)
public class PurchasesControllerTest {

    Mockery context = new JUnit4Mockery();
    CheckoutAssistant checkoutAssistant = context.mock(CheckoutAssistant.class);
    PaymentCollector paymentCollector = context.mock(PaymentCollector.class);
    Cart cart = new Cart();
    PurchasesController controller = new PurchasesController(cart, checkoutAssistant, paymentCollector);

    @Test public void
    checkoutCartsAndRendersPurchaseForm() {
        final Order order = anOrder().build();
        context.checking(new Expectations() {{
            oneOf(checkoutAssistant).checkout(cart);
            will(returnValue(order));
        }});
        ModelAndView modelAndView = controller.checkout();
        assertThat(modelAndView.getViewName(), equalTo("purchases/new"));
        assertThat(modelAndView.getModel(), hasAttribute("order", order));
    }

    @Test public void
    makesCreditCardTypesAvailableToView() {
        assertThat(controller.getCreditCardTypes(), equalTo(CreditCard.Type.values()));
    }

    @Test public void
    collectsPaymentAndRedirectToReceiptView() {
        final Order order = anOrder().withNumber("12345678").build();

        final PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.getBillingAccount().setFirstName("John");
        paymentDetails.getBillingAccount().setLastName("Leclair");
        paymentDetails.getBillingAccount().setEmailAddress("jleclair@gmail.com");
        paymentDetails.setCardType("visa");
        paymentDetails.setCardNumber("9999 9999 9999");
        paymentDetails.setCardExpiryDate("12/12");

        context.checking(new Expectations() {{
            oneOf(paymentCollector).collectPayment(
                    with(equal(order)),
                    with(samePaymentMethodAs(paymentDetails.getCreditCard())),
                    with(sameAccountInformationAs(paymentDetails.getBillingAccount())));
        }});
        String view = controller.create(order, paymentDetails, dummyBindingOn(paymentDetails));
        assertThat(view, equalTo("redirect:/receipts/" + order.getNumber()));
    }

    private BindingResult dummyBindingOn(PaymentDetails paymentDetails) {
        return new BeanPropertyBindingResult(paymentDetails, "paymentDetails");
    }

    private Matcher<Account> sameAccountInformationAs(Account account) {
        return samePropertyValuesAs(account);
    }

    private Matcher<CreditCard> samePaymentMethodAs(CreditCard paymentMethod) {
        return samePropertyValuesAs(paymentMethod);
    }
}