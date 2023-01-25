package com.bank.core.action;
import com.bank.core.account.Account;
import com.bank.core.account.AccountRepository;
import com.bank.core.action.utils.ActionType;
import com.bank.core.action.utils.NewActionRecord;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.bank.core.action.ActionSpecification.*;

public class ActionController {

    private final ActionRepository actionRepository;
    private final AccountRepository accountRepository;

    public ActionController(ActionRepository actionRepository, AccountRepository accountRepository) {
        this.actionRepository = actionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Action> getActions(
            String eventfulAccount,
            Float amount,
            Float amountGreaterThan,
            Float amountLessThan,
            String actionType,
            HttpServletRequest httpServletRequest
    ) {
        if (httpServletRequest.getParameterMap().values().toArray().length < 1) {
            //no params
            return actionRepository.findAll();
        }

        try{
            Specification<Action> specification = Specification.
                    where(eventfulAccount==null?null:eventfulAccountEqual(eventfulAccount))
                    .and(amount==null?null:amountEqual(amount))
                    .and(amountGreaterThan==null?null:amountGreaterThan(amountGreaterThan))
                    .and(amountLessThan==null?null:amountLessThan(amountLessThan))
                    .and(actionType==null?null:actionTypeIs(ActionType.valueOf(actionType)));
            return actionRepository.findAll(specification);
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more incorrect parameter syntax or pattern");
        }
    }

    public void createAction(NewActionRecord newActionRecord){
        Action action = new Action();
        ActionType actionType;
        try{
            actionType = ActionType.valueOf(newActionRecord.actionType().toUpperCase());
        }catch (Exception e){
            System.out.println(e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid ActionType");
        }
        Account eventfulAccount = accountRepository.findById(newActionRecord.eventfulAccount()).orElseThrow(()-> {
            System.out.println("not found account");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Eventful account does not exist");
        });
        action.setEventfulAccount(eventfulAccount.getAccountNumber());
        float amount;
        try{
            amount = Float.parseFloat(newActionRecord.amount().toString());
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid amount format");
        }

        action.setAmount(amount);
        action.setActionType(actionType);
        switch (newActionRecord.actionType().toUpperCase()) {
            case "DEPOSIT" -> {
                action.setReceiverName(null);
                action.setReceiverReference(eventfulAccount.getAccountNumber().toString()); // should be IBAN
            }
            case "TRANSFER" -> {
                action.setReceiverName(newActionRecord.receiverName());
                action.setReceiverReference(newActionRecord.receiverReference()); // account number
            }
            case "WITHDRAW" -> {
                action.setReceiverReference(newActionRecord.receiverReference()); // should be IBAN;
                action.setReceiverName(newActionRecord.receiverName());
            }
            default -> {
            }
        }
        actionRepository.save(action);
    }
}
